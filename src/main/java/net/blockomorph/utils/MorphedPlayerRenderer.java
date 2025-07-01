package net.blockomorph.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;
import java.util.SortedSet;
import java.util.function.Consumer;

public class MorphedPlayerRenderer {
	private final Minecraft mc = Minecraft.getInstance();
	private final BlockRenderDispatcher blockRenderDispatcher = mc.getBlockRenderer();
	private final EntityRenderDispatcher entityDispatcher = mc.getEntityRenderDispatcher();
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
	private final MultiBufferSource crumblBuffer = mc.renderBuffers().crumblingBufferSource();
	private final RandomSource RANDOM = RandomSource.create();

	public boolean render(boolean translucent, AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, Consumer<Float> shadow) {
		if (player instanceof PlayerAccessor pl) {
			if (pl.isFullActive()) {
				shadow.accept(0.0f);
				posestack.pushPose();
				this.adjustMatrixForPlayer(posestack, pl, player);
				this.renderBlock(translucent, player, posestack, buffer, pl);
				this.renderBlockEntity(player, partialticks, posestack, buffer, pl);
				this.renderBreak(player, posestack, buffer, pl);
				this.renderFrame(posestack, buffer, pl);
				posestack.popPose();
				return true;
			} else if (pl.getTnt() != null && !translucent) {
				this.renderTnt(player, partialticks, posestack, buffer, light, pl);
				return true;
			}
		}
		return false;
	}

	private void renderTnt(AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
		PrimedTnt tnt = pl.getTnt();
		EntityRenderer<Entity, EntityRenderState> rend = (EntityRenderer<Entity, EntityRenderState>) entityDispatcher.getRenderer(pl.getTnt());
		EntityRenderState state = rend.createRenderState(tnt, partialticks);
		try {
			rend.render(state, posestack, buffer, light);
		} catch (Exception e) {
			if (player == Minecraft.getInstance().player && Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc)
				sc.tagException = e.getMessage();
		}
	}

	public void adjustMatrixForPlayer(PoseStack poseStack, PlayerAccessor pl, AbstractClientPlayer player) {
		InPlayerBlockPos minpos = pl.minPos();
		AABB hitbox = player.getBoundingBox();
		Vec3 playerCenter = player.position();

		double offsetX = hitbox.minX - (playerCenter.x + minpos.getX());
		double offsetZ = hitbox.minZ - (playerCenter.z + minpos.getZ());

		poseStack.translate(offsetX, 0.0, offsetZ);
	}

	private BlockPos getPosForOffset(BlockInPlayer2 ctr) {
		return ctr.getPos();
	}

	public void renderBlock(boolean translucent, AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		for (Map.Entry<InPlayerBlockPos, BlockInPlayer2> entry : pl.getBlocksData2().entrySet()) {
			BlockInPlayer2 bl = entry.getValue();
			BlockState st = bl.getBlockState();
			InPlayerBlockPos pos = entry.getKey();
			posestack.pushPose();
			posestack.translate(pos.getX(), pos.getY(), pos.getZ());
			if (st.getBlock() instanceof LiquidBlock) {
				//this.renderLiquid(pos, st, posestack, buffer, pl); //TODO RENDER LIQUID
			} else if (st.getRenderShape() == RenderShape.MODEL) {
				this.renderBlock(translucent, player, st, posestack, buffer, bl, bl.getModelData());
			}
			posestack.popPose();
		}
	}

	private void renderBlock(boolean translucent, AbstractClientPlayer player, BlockState blockstate, PoseStack posestack, MultiBufferSource buffer, BlockInPlayer2 block, ModelData data) {
		Level level = player.level();
		var model = this.blockRenderDispatcher.getBlockModel(blockstate);
		BlockPos offset = this.getPosForOffset(block);
		var modeldata = model.getModelData(level, block.getPos(), blockstate, data);
		for (RenderType renderType : model.getRenderTypes(blockstate, RANDOM, modeldata)) {
			if (translucent != (renderType == RenderType.translucent())) return;
			VertexConsumer vertex = buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
			this.blockRenderDispatcher.getModelRenderer().tesselateBlock(level, model, blockstate, offset, posestack, vertex, true, RANDOM, blockstate.getSeed(offset), OverlayTexture.NO_OVERLAY, modeldata, renderType);
		}
	}

	private void renderLiquid(BlockPos offset, BlockState blockstate, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		//this.dispatcher.renderLiquid(BlockPos.ZERO, pl.getLiquidCachedLevel(), buffer.getBuffer(ItemBlockRenderTypes.getRenderLayer(blockstate.getFluidState())), blockstate, blockstate.getFluidState());
		//PlayerLiquidRenderer.render(pl.player().level(), posestack, buffer, LightTexture.pack(15, 15), blockstate.getFluidState());
	}

	private void renderBlockEntity(AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		for (Map.Entry<InPlayerBlockPos, BlockInPlayer2> entry : pl.getBlocksData2().entrySet()) {
			InPlayerBlockPos pos = entry.getKey();
			posestack.pushPose();
			posestack.translate(pos.getX(), pos.getY(), pos.getZ());
			this.renderBlockEntity(entry.getValue(), player, partialticks, posestack, buffer, this.getRenderLight(player, this.getPosForOffset(entry.getValue())), pl);
			posestack.popPose();
		}
	}

	private void renderBlockEntity(BlockInPlayer2 data, AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
		BlockEntity blockEntity = data.getBlockEntity();
		if (blockEntity != null) {
			posestack.pushPose();
			try {
				BlockEntityRenderer<BlockEntity> renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
				if (renderer != null) {
					MultiBufferSource src = buffer;
					int k = getBrakeProgress(blockEntity.getBlockPos());
					if (k > -1 && k < 10) {
						PoseStack.Pose posestack$pose = posestack.last();
						VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(crumblBuffer.getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose, 1.0F);
						src = (p_234298_) -> {
							VertexConsumer vertexconsumer2 = buffer.getBuffer(p_234298_);
							return p_234298_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer2) : vertexconsumer2;
						};
					}
					ClientLevelAccessor acc = ClientLevelAccessor.of(blockEntity.getLevel());
					acc.setBlockEntityRenderingMode(true);
					renderer.render(blockEntity, partialticks, posestack, src, light, OverlayTexture.NO_OVERLAY);
					acc.setBlockEntityRenderingMode(false);
				}
			} catch (Exception e) {
				if (player == Minecraft.getInstance().player && Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc)
					sc.tagException = e.getMessage() == null ? e.getClass().toString() : e.getMessage();
			} finally {
				posestack.popPose();
			}
		}
	}

	private int getRenderLight(AbstractClientPlayer entity, BlockPos blockPos) {
		Level lv = entity.level();
		return LightTexture.pack(lv.getBrightness(LightLayer.BLOCK, blockPos), lv.getBrightness(LightLayer.SKY, blockPos));
	}

	private void renderBreak(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		for (BlockInPlayer2 block : pl.getBlocksData2().values()) {
			BlockState state = block.getBlockState();
			if (state.getRenderShape() == RenderShape.MODEL) {
				int k = getBrakeProgress(block.getPos());
				posestack.pushPose();
				InPlayerBlockPos offset = block.getOffset();

				posestack.translate(offset.getX(), offset.getY(), offset.getZ());
				this.renderBreak(k, state, player, posestack, buffer, block.getModelData(), block);

				posestack.popPose();
			}
		}
	}

	public static int getBrakeProgress(BlockPos bounded) {
		LevelRendererAccessor acc = LevelRendererAccessor.of(Minecraft.getInstance().levelRenderer);
		SortedSet<BlockDestructionProgress> pos = acc.getBrakingBlocks().get(bounded.asLong());
		if (pos != null) {
			BlockDestructionProgress progress = pos.last();
			if (progress != null) {
				return progress.getProgress();
			}
		}
		return -1;
	}

	private void renderBreak(int k, BlockState blockstate, AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, ModelData data, BlockInPlayer2 block) {
		posestack.pushPose();
		PoseStack.Pose posestack$pose1 = posestack.last();
		if (k > -1 && k < 10) {
			VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(crumblBuffer.getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1, 1.0F);
			BlockPos offset = this.getPosForOffset(block);
			this.blockRenderDispatcher.getModelRenderer().tesselateBlock(player.level(), this.blockRenderDispatcher.getBlockModel(blockstate), blockstate, offset, posestack, vertexconsumer1, false, RANDOM, blockstate.getSeed(offset), OverlayTexture.NO_OVERLAY, data, null);
		}

		posestack.popPose();
	}

	private void renderFrame(PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
		MorphedPlayerHitResult hit = this.shouldRenderFrame(pl);
		if (hit != null) {
			posestack.pushPose();
			VoxelShape shape = pl.getRenderShape(hit.getOffset(), mc.player);
			if (shape == null) {
				posestack.popPose();
				return;
			}
			int i = mc.options.highContrastBlockOutline().get() ? -11010079 : ARGB.color(102, -16777216);
			ShapeRenderer.renderShape(posestack, buffer.getBuffer(RenderType.lines()), shape, 0, 0, 0, i);
			posestack.popPose();
		}
	}

	private MorphedPlayerHitResult shouldRenderFrame(PlayerAccessor pl) {
		if (mc.hitResult instanceof MorphedPlayerHitResult hit && !mc.options.hideGui && hit.getPlayer() == pl) {
			if (mc.player.isSpectator()) {
				return MorphUtils.canOpenMenuIn(hit.getPlayer(), hit.getOffset()) ? hit : null;
			} else if (mc.gameMode.getPlayerMode() == GameType.ADVENTURE) {
				return MorphUtils.isAdventureCanBreak(hit.getPlayer(), mc.player, hit.getOffset()) ? hit : null;
			}
			return hit;
		}
		return null;
	}
}
