package net.blockomorph.core.render.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.core.render.dispatch.ContextDependStateExtractor;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MultiBufferSourceWrapper implements SortedRenderOutput {//todo sort
	public static final MultiBufferSourceWrapper INSTANCE = new MultiBufferSourceWrapper(GuiUtils.MC.renderBuffers()::bufferSource);
	private static final BlockPos.MutableBlockPos MUTABLE_BLOCK_POS = new BlockPos.MutableBlockPos();
	private final Supplier<MultiBufferSource> multiBufferSource;

	private MultiBufferSourceWrapper(Supplier<MultiBufferSource> collector) {
		this.multiBufferSource = collector;
	}

	@Override
	public void appendCustomVertex(PoseStack forCopy, RenderType renderType, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor) {
		var consumer = this.multiBufferSource.get().getBuffer(renderType);
		vertexAcceptor.accept(forCopy.last(), consumer);
	}

	@Override
	public void appendChunkMovingBlocks(PoseStack stack, PlayerSectionLayer layer, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor) {
		var consumer = this.multiBufferSource.get().getBuffer(BakedBlocksRenderer.layerToRenderType(layer));
		vertexAcceptor.accept(stack.last(), consumer);
	}

	@Override
	public void appendBreaks(PoseStack poseStack, BlockState block, long seed, int progress) {
		BlockStateModel model = GuiUtils.MC.getModelManager().getBlockModelShaper().getBlockModel(block);
		var renderType = ModelBakery.DESTROY_TYPES.get(progress);
		var buf = GuiUtils.MC.renderBuffers().crumblingBufferSource();
		VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(buf.getBuffer(renderType), poseStack.last(), 1);
		ModelBlockRenderer.renderModel(poseStack.last(), vertexConsumer, model, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
	}

	@Override
	public <T> void appendEntityObject(T gameObjectOrState, PoseStack stack) {
		if (gameObjectOrState instanceof TntRenderState tntRenderState) {
			this.submitTnt(tntRenderState, stack);
		} else if (gameObjectOrState instanceof ContextDependStateExtractor.MorphedBlockEntity blockEntityRenderState) {
			this.submitBlockEntity(blockEntityRenderState, stack);
		} else
			throw new IncompatibleClassChangeError("Cannot findFluid action for object: " + gameObjectOrState);
	}

	private void submitBlockEntity(ContextDependStateExtractor.MorphedBlockEntity blockEntity, PoseStack posestack) {
		if (blockEntity != null) {
			posestack.pushPose();
			var level = LevelWithFlags.of(blockEntity.blockEntity().getLevel());
			try {
				if (!(level instanceof Level lv)) return;
				BlockEntityRenderer<BlockEntity> renderer = GuiUtils.MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity.blockEntity());
				if (renderer != null) {
					level.flags().flywheelDisabled = true;
					int light = LevelRenderer.getLightColor(lv, blockEntity.blockEntity().getBlockPos());
					renderer.render(blockEntity.blockEntity(), blockEntity.deltaTick(), posestack,
							this.prepareBufferForBlockEntity(posestack.last(), blockEntity.brakeProgress()),
							light, OverlayTexture.NO_OVERLAY, blockEntity.morphCam());
				}
			} catch (Exception ignored) {} finally {
				level.flags().flywheelDisabled = false;
				posestack.popPose();
			}
		}
	}

	private MultiBufferSource prepareBufferForBlockEntity(PoseStack.Pose stack, int progress) {
		if (progress < 0 || progress > 9) return this.multiBufferSource.get();
		return renderType -> {
			var breakType = ModelBakery.DESTROY_TYPES.get(progress);
			var buf = GuiUtils.MC.renderBuffers().crumblingBufferSource();
			VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(buf.getBuffer(breakType), stack, 1);
			VertexConsumer original = this.multiBufferSource.get().getBuffer(renderType);
			return renderType.affectsCrumbling() ? VertexMultiConsumer.create(original, vertexConsumer) : original;
		};
	}

	private <S extends TntRenderState> void submitTnt(S state, PoseStack poseStack) {
		if (GuiUtils.MC.level == null) return;
		EntityRenderer<?, ? super S> rend = GuiUtils.MC.getEntityRenderDispatcher().getRenderer(state);
		MUTABLE_BLOCK_POS.set(state.x, state.y, state.z);
		try {
			int light = LevelRenderer.getLightColor(GuiUtils.MC.level, MUTABLE_BLOCK_POS);
			rend.render(state, poseStack, this.multiBufferSource.get(), light);
		} catch (Exception ignored) {}
	}
}
