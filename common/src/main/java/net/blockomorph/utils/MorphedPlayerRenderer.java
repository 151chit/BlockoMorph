package net.blockomorph.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.accessors.compat.SpriteRunner;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.SortedSet;
import java.util.function.Consumer;

public class MorphedPlayerRenderer {
	static final Minecraft MC = Minecraft.getInstance();
	final RandomSource RANDOM = RandomSource.create();

	public boolean render(AbstractClientPlayer player, float anim, float partialTicks, PoseStack posestack, MultiBufferSource buffer, int light, Consumer<Float> shadow) {
		if (player instanceof PlayerAccessor pl) {
			if (pl.isFullActive()) {
				shadow.accept(0.0f);
				posestack.pushPose();
				this.adjustMatrixForPlayer(posestack, pl);
				for (BlockInPlayer2 block : pl.getBlocksData2().values()) {
					posestack.pushPose();
					InPlayerBlockPos pos = block.getOffset();
					posestack.translate(pos.getX(), pos.getY(), pos.getZ());
					this.renderMainBlock(false, posestack, buffer, pl, block);
					this.renderBlockEntity(block, partialTicks, posestack, buffer, pl);
					this.renderBreak(pl, posestack, block);
					posestack.popPose();
				}
				posestack.popPose();
				return true;
			} else if (pl.getTnt() != null) {
				this.renderTnt(anim, partialTicks, posestack, buffer, light, pl);
				return true;
			}
		}
		return false;
	}

	public void renderTranslucentBlocks(PlayerAccessor pl, PoseStack posestack, MultiBufferSource buffer) {
		posestack.pushPose();
		this.adjustMatrixForPlayer(posestack, pl);
		for (BlockInPlayer2 block : pl.getBlocksData2().values()) {
			posestack.pushPose();
			InPlayerBlockPos pos = block.getOffset();
			posestack.translate(pos.getX(), pos.getY(), pos.getZ());
			this.renderMainBlock(true, posestack, buffer, pl, block);
			posestack.popPose();
		}
		this.renderFrame(posestack, buffer, pl);
		posestack.popPose();
	}

	private void renderTnt(float anim, float partialTicks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
		PrimedTnt tnt = pl.getTnt();
		EntityRenderer<? super PrimedTnt> rend = MC.getEntityRenderDispatcher().getRenderer(pl.getTnt());
		try {
			rend.render(tnt, anim, partialTicks, posestack, buffer, light);
		} catch (Exception ignored) {
		}
	}

	public static int getBrakeProgress(BlockPos bounded) {
		SortedSet<BlockDestructionProgress> pos = LevelRendererAccessor.of(MC.levelRenderer).getBrakingBlocks().get(bounded.asLong());
		if (pos != null) {
			BlockDestructionProgress progress = pos.last();
			if (progress != null) {
				return progress.getProgress();
			}
		}
		return -1;
	}

	private int getBrakeProgress(PlayerAccessor pl, BlockPos bounded) {
		if (Config.get().hitReaction.getValue() != ConfigEnums.HitReaction.BRAKING) {
			Player player = pl.player();
			float percentage = player.getHealth() / player.getMaxHealth();
			if (percentage < 0.03) return 9;
			int k = Mth.ceil(percentage * 10f);
			return 9 - k;
		}
		return getBrakeProgress(bounded);
	}

	private MorphedPlayerHitResult shouldRenderFrame(PlayerAccessor pl) {
		if (MC.hitResult instanceof MorphedPlayerHitResult hit && !MC.options.hideGui && hit.getPlayer() == pl && MC.player != null && MC.gameMode != null) {
			if (MC.player.isSpectator()) {
				return MorphUtils.canOpenMenuIn(hit.getPlayer(), hit.getOffset()) ? hit : null;
			} else if (MC.gameMode.getPlayerMode() == GameType.ADVENTURE) {
				return MorphUtils.isAdventureCanBreak(hit.getPlayer(), MC.player, hit.getOffset()) ? hit : null;
			}
			return hit;
		}
		return null;
	}

	private void adjustMatrixForPlayer(PoseStack poseStack, PlayerAccessor pl) {
		InPlayerBlockPos minpos = pl.minPos();
		Player player = pl.player();
		AABB hitbox = player.getBoundingBox();
		Vec3 playerCenter = player.position();

		double offsetX = hitbox.minX - (playerCenter.x + minpos.getX());
		double offsetZ = hitbox.minZ - (playerCenter.z + minpos.getZ());

		poseStack.translate(offsetX, 0.0, offsetZ);
	}

	private void renderMainBlock(boolean translucent, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl, BlockInPlayer2 block) {
		posestack.pushPose();
		try {
			if (block.getBlockState().getRenderShape() == RenderShape.MODEL) {
				ClientPlatformUtils.INSTANCE.renderBlockInWorld(translucent, posestack, buffer, pl, block, RANDOM);
			}
			if (block.shouldDoFluidAction()) {
				this.renderLiquid(translucent, block, posestack, buffer, pl);
			}
		} catch (Exception ignored) {
		} finally {
			posestack.popPose();
		}
	}

	private void renderLiquid(boolean translucent, BlockInPlayer2 block, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		FluidState fluidState = block.getBlockState().getFluidState();
		if (!fluidState.isEmpty()) {
			BlockPos pos = block.getPos();

			RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
			if (renderType == RenderType.translucent()) renderType = RenderType.translucentMovingBlock();
			if (translucent != (renderType == RenderType.translucentMovingBlock())) return;

			float xOffset = (float) (pos.getX() & 15);
			float yOffset = (float) (pos.getY() & 15);
			float zOffset = (float) (pos.getZ() & 15);

			VertexConsumer vertexConsumer = new VertexConsumerWrapper(buffer.getBuffer(renderType)) {
				@Override
				public VertexConsumer addVertex(float x, float y, float z) {
					Vector3f realPos = posestack.last().pose().transformPosition(x - xOffset, y - yOffset, z - zOffset, new Vector3f());
					return super.addVertex(realPos.x(), realPos.y(), realPos.z());
				}
			};

			ClientLevelAccessor acc = ClientLevelAccessor.of(pl.player().level());
			try {
				acc.lockExternalMorphedBlockGetter(true);
				TextureAtlasSprite[] sprites = FogLiquidModifier.getPlatformFluidSprite(pl.player().level(), block);
				for (TextureAtlasSprite sprite : sprites) {
					if (sprite != null && sprite.contents() instanceof SpriteRunner runner) {
						runner.run$blockomorph();
					}
				}
				MC.getBlockRenderer().renderLiquid(pos, pl.player().level(), vertexConsumer, block.getBlockState(), fluidState);
				acc.lockExternalMorphedBlockGetter(false);
			} catch (ReportedException ignored) {
			}
		}
	}

	private void renderBlockEntity(BlockInPlayer2 data, float partialTicks, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		BlockEntity blockEntity = data.getBlockEntity();
		if (blockEntity != null) {
			posestack.pushPose();
			try {
				BlockEntityRenderer<BlockEntity> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
				if (renderer != null) {
					MultiBufferSource bufferSourceWrapper = buffer;
					int k = this.getBrakeProgress(pl, blockEntity.getBlockPos());
					if (k > -1 && k < 10) {
						PoseStack.Pose posestack$pose = posestack.last();
						VertexConsumer wrapped = new SheetedDecalTextureGenerator(MC.renderBuffers().crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose, 1.0F);
						bufferSourceWrapper = (shader) -> {
							VertexConsumer original = buffer.getBuffer(shader);
							return shader.affectsCrumbling() ? VertexMultiConsumer.create(wrapped, original) : original;
						};
					}
					ClientLevelAccessor acc = ClientLevelAccessor.of(blockEntity.getLevel());
					acc.setSpecialRenderingMode(true);
					MC.getBlockEntityRenderDispatcher().render(blockEntity, partialTicks, posestack, bufferSourceWrapper);
					acc.setSpecialRenderingMode(false);
				}
			} catch (Exception ignored) {
			} finally {
				posestack.popPose();
			}
		}
	}

	private void renderBreak(PlayerAccessor pl, PoseStack posestack, BlockInPlayer2 block) {
		posestack.pushPose();
		PoseStack.Pose current = posestack.last();
		BlockPos pos = block.getPos();
		int k = this.getBrakeProgress(pl, pos);
		if (k > -1 && k < 10) {
			VertexConsumer wrapped = new SheetedDecalTextureGenerator(MC.renderBuffers().crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k)), current, 1.0F);
			ClientPlatformUtils.INSTANCE.renderBrake(posestack, wrapped, pl, block, RANDOM);
		}
		posestack.popPose();
	}

	private void renderFrame(PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		MorphedPlayerHitResult hit = this.shouldRenderFrame(pl);
		if (hit != null) {
			posestack.pushPose();
			VoxelShape shape = pl.getRenderShape(hit.getOffset(), MC.player);
			if (shape != null) {
				LevelRendererAccessor.of(MC.levelRenderer).renderBlockHitbox(posestack, buffer.getBuffer(RenderType.lines()), shape, 0, 0, 0, 0f, 0f, 0f, 0.4f);
			}
			posestack.popPose();
		}
	}
}
