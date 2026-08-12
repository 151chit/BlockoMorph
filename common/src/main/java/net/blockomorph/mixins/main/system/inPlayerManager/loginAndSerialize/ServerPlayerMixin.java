package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import net.blockomorph.core.PlayerAccessor;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PlayerAccessor {

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	public void restoreBlockMorphData(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
		PlayerAccessor oldPl = PlayerAccessor.of(oldPlayer);
		this.changeManager(oldPl.getManager());
	}
}
