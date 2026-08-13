package net.blockomorph.mixins.main.system.inPlayerManager.damageHandler;

import net.blockomorph.core.misc.DamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	public void hurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (DamageHandler.needSuppressDamage(source, damage, (Entity) (Object)this)) cir.setReturnValue(false);
	}

}