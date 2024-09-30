package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.blockomorph.utils.MorphUtils;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

@Mixin(Gui.class)
public class HudMixin {

   @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
   private void renderHearts(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
   	  if (MorphUtils.onHudRender(guiGraphics)) ci.cancel();
   }
}
