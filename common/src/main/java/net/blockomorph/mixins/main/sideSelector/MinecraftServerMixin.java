package net.blockomorph.mixins.main.sideSelector;

import net.blockomorph.utils.side.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin<T extends Runnable> extends BlockableEventLoop<T> {

	protected MinecraftServerMixin(String name, boolean propagatesCrashes) {
		super(name, propagatesCrashes);
	}

	@Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/SavedDataStorage;close()V"))
	private void stop(CallbackInfo ci) {
		Side.SERVER.mark(null);
	}

	@Inject(method = "runServer", at = @At("HEAD"))
	private void onRunServer(CallbackInfo ci) {
		Side.SERVER.mark(this);
	}
}