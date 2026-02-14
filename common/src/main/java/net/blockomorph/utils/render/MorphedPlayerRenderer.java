package net.blockomorph.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.utils.VertexConsumerWrapper;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.compat.BlockEntitySpecialRenderer;
import net.blockomorph.utils.accessors.compat.SpriteRunner;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2d;
import org.joml.Vector3f;

public class MorphedPlayerRenderer {
	static final Minecraft MC = Minecraft.getInstance();
	final RandomSource RANDOM = RandomSource.create();

	public boolean submitMorphedState(PoseStack posestack, MorphedPlayerRenderState morphedPlayerRenderState, SubmitNodeCollector collector, CameraRenderState cam) {
		if (morphedPlayerRenderState == null) return false;
		if (morphedPlayerRenderState.morphedState instanceof MorphedPlayerRenderState.BlockMorphedState state) {
			Vector2d vector2d = state.matrixOffset;

			posestack.pushPose();
			posestack.translate(vector2d.x, 0, vector2d.y);

			for (MorphedPlayerRenderState.BlockInfo block : state.blocks) {
				posestack.pushPose();
				InPlayerBlockPos pos = block.offset;
				posestack.translate(pos.getX(), pos.getY(), pos.getZ());

				this.submitMainBlock(false, state.level, block, posestack, collector);
				this.submitBlockEntity(block, posestack, collector, morphedPlayerRenderState.deltaTick, cam);
				this.submitBrake(block, state.level, posestack, collector);

				posestack.popPose();
			}

			posestack.popPose();
			return true;
		} else if (morphedPlayerRenderState.morphedState instanceof MorphedPlayerRenderState.TntMorphedState state) {
			this.submitTnt(state.tntRenderState, posestack, collector, cam);
			return true;
		}
		return false;
	}

	private <S extends EntityRenderState> void submitTnt(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cam) {
		EntityRenderer<?, ? super S> rend = MC.getEntityRenderDispatcher().getRenderer(state);
		try {
			rend.submit(state, poseStack, collector, cam);
		} catch (Exception ignored) {
		}
	}

	public void submitTranslucentBlocks(MorphedPlayerRenderState.BlockMorphedState state, PoseStack posestack, SubmitNodeCollector collector) {
		Vector2d vector2d = state.matrixOffset;

		posestack.pushPose();
		posestack.translate(vector2d.x, 0, vector2d.y);
		for (MorphedPlayerRenderState.BlockInfo block : state.blocks) {
			posestack.pushPose();
			InPlayerBlockPos pos = block.offset;
			posestack.translate(pos.getX(), pos.getY(), pos.getZ());
			this.submitMainBlock(true, state.level, block, posestack, collector);
			posestack.popPose();
		}
		this.submitFrame(state.framedBlock, posestack, collector);
		posestack.popPose();
	}

	private void submitMainBlock(boolean translucent, BlockAndTintGetter level, MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector) {
		posestack.pushPose();
		if (blockInfo.blockState.getRenderShape() == RenderShape.MODEL) {
			ClientPlatformUtils.INSTANCE.submitBlockInWorld(translucent, level, blockInfo.blockState, blockInfo.keyPos, posestack, collector, RANDOM);
		}
		if (blockInfo.needFluidAction) {
			this.submitLiquid(translucent, level, blockInfo, posestack, collector);
		}
		posestack.popPose();
	}

	private void submitLiquid(boolean translucent, BlockAndTintGetter level, MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector) {
		FluidState fluidState = blockInfo.blockState.getFluidState();
		if (!fluidState.isEmpty()) {
			BlockPos pos = blockInfo.keyPos;

			ChunkSectionLayer layer = ItemBlockRenderTypes.getRenderLayer(fluidState);
			RenderType renderType;
			switch (layer) {
				case CUTOUT -> renderType = RenderTypes.cutoutMovingBlock();
				case TRANSLUCENT -> renderType = RenderTypes.translucentMovingBlock();
				case TRIPWIRE -> renderType = RenderTypes.tripwireMovingBlock();
				default -> renderType = RenderTypes.solidMovingBlock();
			}
			if (translucent != (renderType == RenderTypes.translucentMovingBlock())) return;

			float xOffset = (float) (pos.getX() & 15);
			float yOffset = (float) (pos.getY() & 15);
			float zOffset = (float) (pos.getZ() & 15);

			collector.submitCustomGeometry(posestack, renderType, (pose, vertexConsumer) -> {
				VertexConsumer vertexConsumerWrapper = new VertexConsumerWrapper(vertexConsumer) {
					@Override
					public VertexConsumer addVertex(float x, float y, float z) {
						Vector3f realPos = pose.pose().transformPosition(x - xOffset, y - yOffset, z - zOffset, new Vector3f());
						return super.addVertex(realPos.x(), realPos.y(), realPos.z());
					}
				};

				ClientLevelAccessor acc = ClientLevelAccessor.of(level);
				try {
					acc.lockExternalMorphedBlockGetter(true);
					this.activateSprite(level, blockInfo);
					MC.getBlockRenderer().renderLiquid(pos, level, vertexConsumerWrapper, blockInfo.blockState, fluidState);
					acc.lockExternalMorphedBlockGetter(false);
				} catch (ReportedException ignored) {
				}
			});
		}
	}

	private void activateSprite(BlockAndTintGetter blockAndTintGetter, MorphedPlayerRenderState.BlockInfo blockInfo) {
		TextureAtlasSprite[] sprites = FogLiquidModifier.getPlatformFluidSprite(blockAndTintGetter, blockInfo.blockState, blockInfo.keyPos);
		if (sprites != null) {
			for (TextureAtlasSprite sprite : sprites) {
				if (sprite != null && sprite.contents() instanceof SpriteRunner runner) {
					runner.run$blockomorph();
				}
			}
		}
	}

	private void submitBlockEntity(MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector, float deltaTick, CameraRenderState cam) {
		BlockEntity blockEntity = blockInfo.blockEntity;
		if (blockEntity != null) {
			posestack.pushPose();
			try {
				BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
				if (renderer != null) {
					int k = blockInfo.brakeProgress;
					ModelFeatureRenderer.CrumblingOverlay overlay;
					if (k > -1 && k < 10) {
						overlay = new ModelFeatureRenderer.CrumblingOverlay(k, posestack.last().copy());
					} else {
						overlay = null;
					}

					ClientLevelAccessor acc = ClientLevelAccessor.of(blockEntity.getLevel());
					acc.setSpecialRenderingMode(true);

					BlockEntitySpecialRenderer.tryRenderWithoutOptimizations(() -> {
						BlockEntityRenderState state = renderer.createRenderState();
						renderer.extractRenderState(blockEntity, state, deltaTick, cam.pos, overlay);
						renderer.submit(state, posestack, collector, cam);
					});

					acc.setSpecialRenderingMode(false);
				}
			} catch (Exception ignored) {
			} finally {
				posestack.popPose();
			}
		}
	}

	private void submitBrake(MorphedPlayerRenderState.BlockInfo blockInfo, BlockAndTintGetter level, PoseStack posestack, SubmitNodeCollector collector) {
		int k = blockInfo.brakeProgress;
		if (k > -1 && k < 10) {
			posestack.pushPose();

			collector.submitCustomGeometry(posestack, RenderTypes.LINES, (posestack$pose1, ignored) -> {
				VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(MC.renderBuffers().crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1, 1.0F);
				PoseStack poseStack = new PoseStack();
				poseStack.last().set(posestack$pose1);
				MC.getBlockRenderer().renderBreakingTexture(blockInfo.blockState, blockInfo.keyPos, level, poseStack, vertexconsumer1);
			});

			posestack.popPose();
		}
	}

	private void submitFrame(VoxelShape shape, PoseStack posestack, SubmitNodeCollector collector) {
		if (shape != null) {
			posestack.pushPose();
			int i = MC.options.highContrastBlockOutline().get() ? -11010079 : ARGB.color(102, -16777216);
			collector.submitCustomGeometry(posestack, RenderTypes.LINES, ((pose, vertexConsumer) -> {
				PoseStack poseStack = new PoseStack();
				poseStack.last().set(pose);
				ShapeRenderer.renderShape(poseStack, vertexConsumer, shape, 0, 0, 0, i, MC.getWindow().getAppropriateLineWidth());
			}));
			posestack.popPose();
		}
	}
}
