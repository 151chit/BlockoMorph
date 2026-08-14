package net.blockomorph.screens.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.blockomorph.core.levelFlags.MorphedLevelFeatureFlags;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.accessors.LightningSetter;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static net.blockomorph.screens.utils.GuiUtils.*;

public class GuiBlockRenderer extends PictureInPictureRenderer<GuiBlockRenderState> {
	private final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(true, false, MC.getBlockColors());
	public GuiBlockRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<GuiBlockRenderState> getRenderStateClass() {
		return GuiBlockRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiBlockRenderState guiState, PoseStack stack) {
		LightningSetter.renderWithLight(() -> {
			stack.mulPose(Axis.XP.rotationDegrees(30.0F));
			stack.mulPose(Axis.YP.rotationDegrees(-45.0F));
			stack.mulPose(Axis.ZP.rotationDegrees(180.0F));

			BlockEntity blockEntity = guiState.getBlockEntity();
			try { this.renderBlockInGui(bufferSource, stack, guiState.getState(), blockEntity != null ? blockEntity.getBlockPos() : AIR);
			} catch (Throwable ignored) {}
			try { this.renderBlockEntity(guiState.getDeltaTick(), stack, blockEntity);
			} catch (Throwable ignored) {}
		}, DIFFUSE_LIGHT_START, DIFFUSE_LIGHT_END);
	}

	private void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {
		var level = MC.level;
		if (blockState.getRenderShape() == RenderShape.MODEL && level != null) {
			BlockStateModel model = MC.getModelManager().getBlockStateModelSet().get(blockState);
			LevelWithFlags acc = LevelWithFlags.of(level);
			acc.flags().customLightProvider = MorphedLevelFeatureFlags.LightProvider.ALWAYS_LIGHT;
			BlockQuadOutput quadOutput = (xOff, yOff, zOff, quad, instance) -> {
				VertexConsumer builder = bufferSource.getBuffer(PlayerSectionLayer.byChunkType(quad.materialInfo().layer()).renderType());
				stack.pushPose();
				stack.translate(xOff, yOff, zOff);
				builder.putBakedQuad(stack.last(), quad, instance);
				stack.popPose();
			};
			try {
				this.blockRenderer.tesselateBlock(quadOutput, 0, 0, 0, level, zeroOrFake, blockState, model, blockState.getSeed(zeroOrFake));
			} finally {
				acc.flags().customLightProvider = null;
			}
		}
	}

	@Override
	protected void blitTexture(GuiBlockRenderState pictureInPictureRenderState, GuiRenderState guiRenderState) {
		super.blitTexture(pictureInPictureRenderState, guiRenderState);
		LightningSetter.disableLigth();
	}

	private <T extends BlockEntity, S extends BlockEntityRenderState> void renderBlockEntity(float delta, PoseStack stack, T blockEntity) {
		if (blockEntity != null) {
			BlockEntityRenderer<T, S> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
			if (renderer != null) {
				LevelWithFlags acc = LevelWithFlags.of(MC.level);
				try {
					Camera cam = MC.gameRenderer.getMainCamera();
					acc.flags().flywheelDisabled = true;
					acc.flags().customLightProvider = MorphedLevelFeatureFlags.LightProvider.ALWAYS_LIGHT;
					FeatureRenderDispatcher renderDispatcher = MC.gameRenderer.getFeatureRenderDispatcher();
					S state = renderer.createRenderState();
					renderer.extractRenderState(blockEntity, state, delta, cam.position(), null);
					state.lightCoords = 15728880;
					renderer.submit(state, stack, renderDispatcher.getSubmitNodeStorage(), MC.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState);
					renderDispatcher.renderAllFeatures();
				} catch (Exception ignored) {} finally {
					acc.flags().flywheelDisabled = false;
					acc.flags().customLightProvider = null;
				}
			}
		}
	}

	@Override
	protected float getTranslateY(int length, int height) {
		return (float) length / 2;
	}

	@Override
	protected String getTextureLabel() {
		return MorphUtils.res("block and blockentity").toString();
	}
}
