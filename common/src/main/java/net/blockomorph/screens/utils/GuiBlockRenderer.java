package net.blockomorph.screens.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockomorph.core.levelFlags.MorphedLevelFeatureFlags;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.accessors.LightningSetter;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
			try { this.renderBlockInGui(bufferSource, stack, guiState.getState(), blockEntity != null ? blockEntity.getBlockPos() : AIR);
			} catch (Throwable ignored) {}
			try { this.renderBlockEntity(guiState.getDeltaTick(), stack, blockEntity);
			} catch (Throwable ignored) {}
		}, DIFFUSE_LIGHT_START, DIFFUSE_LIGHT_END);
	}

	private void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {//todo
		var level = MC.level;
		if (blockState.getRenderShape() == RenderShape.MODEL && level != null) {
			BlockStateModel model = MC.getModelManager().getBlockModelShaper().getBlockModel(blockState);
			LevelWithFlags acc = LevelWithFlags.of(level);
			acc.flags().customLightProvider = MorphedLevelFeatureFlags.LightProvider.ALWAYS_LIGHT;
			try {
				RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
				RenderType simplified = ItemBlockRenderTypes.getMovingBlockRenderType(blockState);
				MC.getBlockRenderer().getModelRenderer().tesselateBlock(MC.level,
						model.collectParts(random), blockState, zeroOrFake, stack, bufferSource.getBuffer(simplified), false, OverlayTexture.NO_OVERLAY);
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

	private <T extends BlockEntity> void renderBlockEntity(float delta, PoseStack stack, T blockEntity) {
		if (blockEntity != null) {
			BlockEntityRenderer<T> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
			if (renderer != null) {
				LevelWithFlags level = LevelWithFlags.of(MC.level);
				try {
					Camera cam = MC.getBlockEntityRenderDispatcher().camera;
					level.flags().flywheelDisabled = true;
					renderer.render(blockEntity, delta, stack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, cam.getPosition());
				} catch (Exception ignored) {} finally {
					level.flags().flywheelDisabled = false;
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
