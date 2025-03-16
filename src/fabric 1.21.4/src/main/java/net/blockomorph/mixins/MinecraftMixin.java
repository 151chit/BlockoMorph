package net.blockomorph.mixins;

import net.blockomorph.utils.MorphUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Shadow
	private int rightClickDelay;

   @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
   private void startAttack(CallbackInfoReturnable<Boolean> cir) {
   	 Minecraft mc = (Minecraft)(Object) this;
   	 ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
   	 if (mc.missTime == 0 && !mc.player.isHandsBusy() && itemStack.isItemEnabled(mc.level.enabledFeatures())) {
   	   if (MorphUtils.onAttackBlockPlayer()) cir.setReturnValue(false);
   	 }
   }

   @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
   private void use(CallbackInfo ci) {
		Minecraft mc = (Minecraft)(Object) this;
		if (!mc.gameMode.isDestroying()) {
			this.rightClickDelay = 4;
			if (!mc.player.isHandsBusy() && mc.hitResult != null) {
				boolean flag = MorphUtils.performClientUse();
				if (flag) ci.cancel();
			}
		}
   }
}
