package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
	private void stop(CallbackInfo ci) {
		PlayerWorldSerializer.terminateAllImmediate();
		BlockPosBounds.releaseServerData();
	}

	@Inject(method = "tickChildren", at = @At("TAIL"))
	private void tick(BooleanSupplier haveTime, CallbackInfo ci) {
		PlayerWorldSerializer.tickAll();
	}

	@Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z"))
	private void start(CallbackInfo ci) {
		BlockPosBounds.load((MinecraftServer) (Object)this);
	}
}
