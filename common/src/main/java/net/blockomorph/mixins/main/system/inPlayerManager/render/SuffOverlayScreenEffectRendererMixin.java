package net.blockomorph.mixins.main.system.inPlayerManager.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.BlockInCamera;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.side.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public class SuffOverlayScreenEffectRendererMixin {

	@WrapMethod(method = "renderScreenEffect")
	private static void handle(Minecraft minecraft, PoseStack poseStack, MultiBufferSource multiBufferSource, Operation<Void> original) {
		var block = BlockInCamera.findBlock(GuiUtils.MC.gameRenderer.getMainCamera(), GuiUtils.MC.level);
		if (block != null) {
			LevelWithFlags.of(GuiUtils.MC.level).flags().overrideBlockGetter = (ignored1, ignoredX, ignoredY, ignoredZ) -> block.getBlockState();
		}
		try {
			original.call(minecraft, poseStack, multiBufferSource);
		} finally {
			LevelWithFlags.of(GuiUtils.MC.level).flags().overrideBlockGetter = null;
		}
	}

	@FastInject(method = "getViewBlockingState", at = @At("HEAD"))
	private static Object redirect(Player player) {
		var getter = LevelWithFlags.of(GuiUtils.MC.level).flags().overrideBlockGetter;
		if (getter != null && Side.get() == Side.CLIENT) {
			return getter.getBlockState(GuiUtils.MC.level, 0, 0, 0);
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}
