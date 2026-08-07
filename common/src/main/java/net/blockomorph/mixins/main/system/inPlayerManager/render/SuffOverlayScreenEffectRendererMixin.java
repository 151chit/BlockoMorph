package net.blockomorph.mixins.main.system.inPlayerManager.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.BlockInCamera;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.side.Side;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public class SuffOverlayScreenEffectRendererMixin {

	@WrapMethod(method = "renderScreenEffect")
	private void handle(boolean isFirstPerson, boolean isSleeping, float partialTicks, SubmitNodeCollector submitNodeCollector, boolean hideGui, Operation<Void> original) {
		var block = BlockInCamera.findBlock(GuiUtils.MC.gameRenderer.getMainCamera(), GuiUtils.MC.level);
		if (block != null) {
			LevelWithFlags.of(GuiUtils.MC.level).flags().overrideBlockGetter = (ignored1, ignoredX, ignoredY, ignoredZ) -> block.getBlockState();
		}
		try {
			original.call(isFirstPerson, isSleeping, partialTicks, submitNodeCollector, hideGui);
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
