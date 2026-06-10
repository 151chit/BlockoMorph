package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void render(GuiGraphicsExtractor GuiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiUtils.renderOverlay(GuiGraphicsExtractor, deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	@Inject(method = "extractHearts", at = @At("HEAD"), cancellable = true)
	private void renderHearts(GuiGraphicsExtractor GuiGraphicsExtractor, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
		if (PlayerAccessor.of(player).isActive()) ci.cancel();
	}
}
