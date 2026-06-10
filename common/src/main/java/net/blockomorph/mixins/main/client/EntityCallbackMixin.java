package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$EntityCallbacks")
public class EntityCallbackMixin {

	@Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void add(Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl) {
			var manager = pl.getSectionHandler();
			if (manager != null) {
				manager.onAdd();
			}
		}
	}

	@Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void onRemove(Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl) {
			var manager = pl.getSectionHandler();
			if (manager != null) {
				manager.onRemove();
			}
		}
	}
}