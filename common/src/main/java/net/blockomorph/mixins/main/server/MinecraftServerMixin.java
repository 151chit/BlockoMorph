package net.blockomorph.mixins.main.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.SideSelector;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.tick.PlayersTickManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;close()V"))
	public void releaseCache(CallbackInfo ci, @Local ServerLevel serverLevel) {
		BlockPosBounds.clearServerData();
	}

	@Inject(method = "tickChildren", at = @At("TAIL"))
	private void tick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
		PlayersTickManager.tick();
	}

	@Inject(method = "stopServer", at = @At("TAIL"))
	private void stop(CallbackInfo ci) {
		PlayersTickManager.reinit();
		Config.setServer(null);
	}

	@Inject(method = "runServer", at = @At("HEAD"))
	private void onRunServer(CallbackInfo ci) {
		SideSelector.markServer();
	}
}
