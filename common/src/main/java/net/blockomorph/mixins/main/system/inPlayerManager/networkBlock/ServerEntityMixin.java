package net.blockomorph.mixins.main.system.inPlayerManager.networkBlock;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
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

	@Inject(method = "addPairing", at = @At(value = "TAIL"))
	public void start(ServerPlayer looker, CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer pl && this.entity instanceof PlayerAccessor acc) {
			BlockPosBounds.refreshForRemoteClient(looker, pl, false);
			acc.sendAllContentToPlayer(looker);
		}
	}

	@Inject(method = "removePairing", at = @At(value = "TAIL"))
	public void stop(ServerPlayer looker, CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer pl) {
			BlockPosBounds.refreshForRemoteClient(looker, pl, true);
		}
	}
}
