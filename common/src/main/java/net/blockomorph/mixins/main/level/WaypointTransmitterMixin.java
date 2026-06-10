package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaypointTransmitter.class)
public interface WaypointTransmitterMixin {

	@Inject(method = "doesSourceIgnoreReceiver", at = @At("HEAD"), cancellable = true)
	private static void checkAccess(LivingEntity livingEntity, ServerPlayer serverPlayer, CallbackInfoReturnable<Boolean> cir) {
		if (livingEntity instanceof PlayerAccessor acc && acc.isActive()) {
			cir.setReturnValue(true);
		}
	}
}
