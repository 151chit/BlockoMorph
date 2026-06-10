package net.blockomorph.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.utils.VertexRecorder;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.compat.BlockEntitySpecialRenderer;
import net.blockomorph.utils.accessors.compat.SpriteRunner;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2d;
import org.joml.Vector3f;

import java.util.EnumMap;

public class MorphedPlayerRenderer {
	static final Minecraft MC = Minecraft.getInstance();
	FluidRenderer fluidRenderer;
	ModelBlockRenderer blockRenderer;

	public boolean submitMorphedState(PoseStack posestack, MorphedPlayerRenderState morphedPlayerRenderState, SubmitNodeCollector collector, CameraRenderState cam) {
		if (morphedPlayerRenderState == null) return false;
		if (morphedPlayerRenderState.morphedState instanceof MorphedPlayerRenderState.BlockMorphedState state) {
			this.assignRenderers();
			Vector2d vector2d = state.matrixOffset;

			posestack.pushPose();
			posestack.translate(vector2d.x, 0, vector2d.y);

			for (MorphedPlayerRenderState.BlockInfo block : state.blocks) {
				posestack.pushPose();
				InPlayerBlockPos pos = block.offset;
				posestack.translate(pos.getX(), pos.getY(), pos.getZ());

				this.submitMainBlock(state.level, block, posestack, collector);
				this.submitBlockEntity(block, posestack, collector, cam);
				this.submitBrake(block, posestack, collector);

				posestack.popPose();
			}
			this.submitFrame(state, posestack, collector);

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
		} catch (Exception ignored) {}
	}

	private void assignRenderers() {
		this.fluidRenderer = new FluidRenderer(MC.getModelManager().getFluidStateModelSet());
		this.blockRenderer = new ModelBlockRenderer(MC.options.ambientOcclusion().get(), true, MC.getBlockColors());
	}

	private void submitMainBlock(BlockAndTintGetter level, MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector) {
		posestack.pushPose();
		if (blockInfo.blockState.getRenderShape() == RenderShape.MODEL) {
			this.submitBlock(level, blockInfo.blockState, blockInfo.keyPos, posestack, collector);
		}
		if (blockInfo.needFluidAction) {
			this.submitLiquid(level, blockInfo, posestack, collector);
		}
		posestack.popPose();
	}

	private void submitBlock(BlockAndTintGetter level, BlockState blockstate, BlockPos keyPos, PoseStack posestack, SubmitNodeCollector collector) {
		BlockStateModel model = MC.getModelManager().getBlockStateModelSet().get(blockstate);
		EnumMap<ChunkSectionLayer, VertexRecorder> buffers = new EnumMap<>(ChunkSectionLayer.class);
		BlockQuadOutput quadOutput = (xOff, yOff, zOff, quad, instance) -> {
			ChunkSectionLayer type = quad.materialInfo().layer();
			type = this.checkRenderType(type, blockstate);
			VertexRecorder vertexRecorder = buffers.computeIfAbsent(type, _ -> new VertexRecorder());
			posestack.pushPose();
			posestack.translate(xOff, yOff, zOff);
			vertexRecorder.putBakedQuad(posestack.last(), quad, instance);
			posestack.popPose();
		};
		this.blockRenderer.tesselateBlock(quadOutput, 0, 0, 0, level, keyPos, blockstate, model, blockstate.getSeed(keyPos));
		buffers.forEach((layer, vertexRecorder) -> {
			collector.order(layer.ordinal()).submitCustomGeometry(posestack, ClientPlatformUtils.layerToRenderType(layer), (_, orig) -> vertexRecorder.replay(orig));
		});
	}

	private ChunkSectionLayer checkRenderType(ChunkSectionLayer orig, BlockState state) {
		if (state.is(Blocks.REDSTONE_WIRE) ||
				state.is(Blocks.GLASS) ||
				state.is(Blocks.GLASS_PANE)
		) return ChunkSectionLayer.CUTOUT;
		if (!MC.options.cutoutLeaves().get() && state.getBlock() instanceof LeavesBlock) return ChunkSectionLayer.SOLID;
		return orig;
	}

	private void submitLiquid(BlockAndTintGetter level, MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector) {
		FluidState fluidState = blockInfo.blockState.getFluidState();
		if (!fluidState.isEmpty()) {
			BlockPos pos = blockInfo.keyPos;
			EnumMap<ChunkSectionLayer, VertexRecorder> buffers = new EnumMap<>(ChunkSectionLayer.class);
			PoseStack.Pose currPose = posestack.last().copy();
			this.activateSprite(level, blockInfo);
			ClientLevelAccessor acc = ClientLevelAccessor.of(level);
			try {
				acc.lockExternalMorphedBlockGetter(true);
				this.fluidRenderer.tesselate(level, pos, this.createVertexReceiverFluid(pos, buffers, currPose), blockInfo.blockState, fluidState);
			} catch (Exception ignored) {} finally {
				acc.lockExternalMorphedBlockGetter(false);
			}
			buffers.forEach((layer, vertexRecorder) -> {
				collector.order(layer.ordinal()).submitCustomGeometry(posestack,
						ClientPlatformUtils.layerToRenderType(layer), (_, origVertexConsumer) -> vertexRecorder.replay(origVertexConsumer));
			});
		}
	}

	private FluidRenderer.Output createVertexReceiverFluid(BlockPos pos, EnumMap<ChunkSectionLayer, VertexRecorder> buffers, PoseStack.Pose currPose) {
		float xOffset = (float) (pos.getX() & 15);
		float yOffset = (float) (pos.getY() & 15);
		float zOffset = (float) (pos.getZ() & 15);
		return (chunkLayer) -> buffers.computeIfAbsent(chunkLayer, _-> new VertexRecorder() {
			@Override
			public VertexConsumer addVertex(float x, float y, float z) {
				Vector3f realPos = currPose.pose().transformPosition(x - xOffset, y - yOffset, z - zOffset, new Vector3f());
				return super.addVertex(realPos.x(), realPos.y(), realPos.z());
			}
		});
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

	private void submitBlockEntity(MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector, CameraRenderState cam) {
		BlockEntityRenderState blockEntity = blockInfo.blockEntity;
		if (blockEntity != null) {
			posestack.pushPose();
			try {
				BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
				if (renderer != null) {
					BlockEntitySpecialRenderer.tryRenderWithoutOptimizations(() -> {
						renderer.submit(blockEntity, posestack, collector, cam);
					});
				}
			} catch (Exception ignored) {} finally {
				posestack.popPose();
			}
		}
	}

	private void submitBrake(MorphedPlayerRenderState.BlockInfo blockInfo, PoseStack posestack, SubmitNodeCollector collector) {
		int k = blockInfo.brakeProgress;
		if (k > -1 && k < 10) {
			posestack.pushPose();

			BlockStateModel model = MC.getModelManager().getBlockStateModelSet().get(blockInfo.blockState);
			collector.submitBreakingBlockModel(posestack, model, blockInfo.blockState.getSeed(blockInfo.keyPos), k);

			posestack.popPose();
		}
	}

	public void submitFrame(MorphedPlayerRenderState.BlockMorphedState state, PoseStack posestack, SubmitNodeCollector collector) {
		VoxelShape shape = state.framedBlock;
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
