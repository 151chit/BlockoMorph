package net.blockomorph.mixins;

import net.blockomorph.utils.MorphUtils;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
   @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
   public void hurt(DamageSource damage, float count, CallbackInfoReturnable<Boolean> cir) {
   	  if (MorphUtils.onPlayerAttacked(damage, (Entity)(Object)this)) cir.setReturnValue(false);
   }
}
