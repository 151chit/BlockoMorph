package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public class EntityCallbackMixin {

	@Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void add(Entity entity, CallbackInfo ci) {
		if (entity instanceof ServerPlayer player && player instanceof PlayerAccessor pl) {
			var manager = pl.getListenersStorage();
			if (manager != null) {
				manager.onMove();
			}
		}
	}

	@Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void onRemove(Entity entity, CallbackInfo ci) {
		if (entity instanceof ServerPlayer player && player instanceof PlayerAccessor pl && player.level() instanceof ServerLevelAccessor acc) {
			var manager = pl.getListenersStorage();
			if (manager != null) {
				manager.onRemove(acc.getPlayerGameEventListenerMap());
			}
		}
	}
}
