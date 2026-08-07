package net.blockomorph.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.side.MinecraftThreadLocal;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SubmitNodeCollectorWrapper implements SortedRenderOutput {
	public static final SubmitNodeCollectorWrapper INSTANCE = new SubmitNodeCollectorWrapper(GuiUtils.MC.gameRenderer::getSubmitNodeStorage);
	private final Supplier<SubmitNodeCollector> submitNodeCollector;
	private final MinecraftThreadLocal<CameraRenderState> cameraRenderState = new MinecraftThreadLocal<>(true, null);

	private SubmitNodeCollectorWrapper(Supplier<SubmitNodeCollector> collector) {
		this.submitNodeCollector = collector;
	}

	public SubmitNodeCollectorWrapper setAdditionalData(CameraRenderState state) {
		this.cameraRenderState.set(state);
		return this;
	}

	@Override
	public void appendCustomVertex(PoseStack forCopy, RenderType renderType, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor) {
		this.submitNodeCollector.get().submitCustomGeometry(forCopy, renderType, vertexAcceptor::accept);
	}

	@Override
	public void appendChunkMovingBlocks(PoseStack stack, PlayerSectionLayer layer, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor) {
		this.submitNodeCollector.get().order(layer.ordinal()).submitCustomGeometry(stack, BakedBlocksRenderer.layerToRenderType(layer), vertexAcceptor::accept);
	}

	@Override
	public void appendBreaks(PoseStack poseStack, BlockState block, long seed, int progress) {
		BlockStateModel model = GuiUtils.MC.getModelManager().getBlockStateModelSet().get(block);
		this.submitNodeCollector.get().submitBreakingBlockModel(poseStack, model, seed, progress);
	}

	@Override
	public <T> void appendEntityObject(T gameObjectOrState, PoseStack stack) {
		if (gameObjectOrState instanceof TntRenderState tntRenderState) {
			this.submitTnt(tntRenderState, stack);
		} else if (gameObjectOrState instanceof BlockEntityRenderState blockEntityRenderState) {
			this.submitBlockEntity(blockEntityRenderState, stack);
		} else
			throw new IncompatibleClassChangeError("Cannot findFluid action for object: " + gameObjectOrState);
	}

	private void submitBlockEntity(BlockEntityRenderState blockEntity, PoseStack posestack) {
		if (blockEntity != null) {
			posestack.pushPose();
			try {
				BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = GuiUtils.MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
				if (renderer != null) {
					renderer.submit(blockEntity, posestack, this.submitNodeCollector.get(), this.cameraRenderState.get());
				}
			} catch (Exception ignored) {} finally {
				posestack.popPose();
			}
		}
	}

	private <S extends TntRenderState> void submitTnt(S state, PoseStack poseStack) {
		EntityRenderer<?, ? super S> rend = GuiUtils.MC.getEntityRenderDispatcher().getRenderer(state);
		try {
			rend.submit(state, poseStack, this.submitNodeCollector.get(), this.cameraRenderState.get());
		} catch (Exception ignored) {}
	}
}
