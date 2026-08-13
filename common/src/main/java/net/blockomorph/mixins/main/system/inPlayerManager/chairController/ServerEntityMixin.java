package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import net.blockomorph.core.misc.chairController.EntityChairController;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
	@Shadow @Final private Entity entity;

	@Inject(method = "addPairing", at = @At("TAIL"))
	private void send(ServerPlayer looker, CallbackInfo ci) {
		if (this.entity instanceof EntityChairController.ChairedEntity ent) {
			var pkt = ent.getController().getSyncPacket();
			if (pkt != null) looker.connection.send(pkt);
		} else if (this.entity instanceof EntityChairController.ChairPlayer ch) {
			ch.getHolder().forEachPassengers(passenger -> {
				var pkt = passenger.getController().getSyncPacket();
				if (pkt != null) looker.connection.send(pkt);
			});
		}
	}
}
