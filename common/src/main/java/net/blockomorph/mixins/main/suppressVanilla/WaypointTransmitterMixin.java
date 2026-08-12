package net.blockomorph.mixins.main.suppressVanilla;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WaypointTransmitter.class)
public interface WaypointTransmitterMixin {

	@FastInject(method = "doesSourceIgnoreReceiver", at = @At("HEAD"))
	private static byte checkAccess(LivingEntity source, ServerPlayer receiver) {
		if (source instanceof PlayerAccessor acc && acc.isBlockomorphActive()) {
			return 1;
		}
		return 0;
	}
}
