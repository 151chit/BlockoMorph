package net.blockomorph.mixins.main.gui;

import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.screens.overlay.Overlay;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiOverlayMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Overlay.renderOverlays(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	@FastInject(method = "extractHearts", at = @At("HEAD"))
	private boolean suppressVanillaHearts(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase, int healthRowHeight, int heartOffsetIndex, float maxHealth, int currentHealth, int oldHealth, int absorption, boolean blink) {
		return PlayersStorage.NOT_MORPHED_PLAYER.test(player);
	}
}
