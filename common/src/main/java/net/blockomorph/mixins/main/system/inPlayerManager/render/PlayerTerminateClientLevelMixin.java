package net.blockomorph.mixins.main.system.inPlayerManager.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class PlayerTerminateClientLevelMixin {

	@Inject(method = "removeEntity", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
	private void remove(CallbackInfo ci, @Local Entity entity) {
		if (entity instanceof AbstractClientPlayer player) {
			PlayersAsyncBakersManager.Provider.getFromVanilla().terminateFor(player.getUUID());
		}
	}
}
