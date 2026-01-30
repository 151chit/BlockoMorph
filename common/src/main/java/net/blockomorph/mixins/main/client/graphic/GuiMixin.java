package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		GuiUtils.renderOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void renderHearts(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
		if (PlayerAccessor.of(player).isActive()) ci.cancel();
	}
}
