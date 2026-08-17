package net.blockomorph.mixins.main.sideSelector;

import net.blockomorph.utils.side.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin<T extends Runnable> extends BlockableEventLoop<T> {
	protected MinecraftMixin(String name) {
		super(name);
	}

	@Inject(require = 1, method = "<init>", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;<init>(Ljava/lang/String;)V", unsafe = true))
	private void run(CallbackInfo ci) {
		Side.CLIENT.mark(this);
	}

	@Inject(method = "destroy", at= @At("HEAD"))
	private void stopFinal(CallbackInfo ci) {
		Side.CLIENT.mark(null);
	}
}