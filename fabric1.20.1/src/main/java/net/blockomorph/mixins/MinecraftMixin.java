package net.blockomorph.mixins;

import net.blockomorph.utils.MorphUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

@Mixin(Minecraft.class)
public class MinecraftMixin {
   @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
   private void startAttack(CallbackInfoReturnable<Boolean> cir) {
   	 Minecraft mc = (Minecraft)(Object) this;
   	 ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
   	 if (mc.missTime == 0 && !mc.player.isHandsBusy() && itemStack.isItemEnabled(mc.level.enabledFeatures())) {
   	   if (MorphUtils.onAttackBlockPlayer()) cir.setReturnValue(false);
   	 }
   }
}
