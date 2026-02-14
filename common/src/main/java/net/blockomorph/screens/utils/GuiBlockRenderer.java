package net.blockomorph.screens.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.LightningSetter;
import net.blockomorph.utils.accessors.compat.BlockEntitySpecialRenderer;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;

import static net.blockomorph.screens.utils.GuiUtils.*;

public class GuiBlockRenderer extends PictureInPictureRenderer<GuiBlockRenderState> {
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
			try { ClientPlatformUtils.INSTANCE.renderBlockInGui(bufferSource, stack, guiState.getState(), blockEntity != null ? blockEntity.getBlockPos() : AIR);
			} catch (Throwable ignored) {}
			try { this.renderBlockEntity(guiState.getDeltaTick(), stack, blockEntity);
			} catch (Throwable ignored) {}
		}, DIFFUSE_LIGHT_START, DIFFUSE_LIGHT_END);
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
				ClientLevelAccessor acc = ClientLevelAccessor.of(MC.level);
				try {
					Camera cam = MC.gameRenderer.getMainCamera();
					acc.setSpecialRenderingMode(true);
					FeatureRenderDispatcher renderDispatcher = MC.gameRenderer.getFeatureRenderDispatcher();
					BlockEntitySpecialRenderer.tryRenderWithoutOptimizations(() -> {
						S state = renderer.createRenderState();
						renderer.extractRenderState(blockEntity, state, delta, cam.position(), null);
						state.lightCoords = LightTexture.FULL_BRIGHT;
						renderer.submit(state, stack, renderDispatcher.getSubmitNodeStorage(), MC.gameRenderer.getLevelRenderState().cameraRenderState);
					});
					renderDispatcher.renderAllFeatures();
				} catch (Exception ignored) {
				} finally {
					acc.setSpecialRenderingMode(false);
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
