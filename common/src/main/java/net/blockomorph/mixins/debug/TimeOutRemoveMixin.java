package net.blockomorph.mixins.debug;

import io.netty.channel.Channel;
import net.blockomorph.utils.EarlyLoadingPlatformService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.Connection$1")
public class TimeOutRemoveMixin {

	@Inject(method = "initChannel", at = @At("TAIL"))
	private void remove(Channel channel, CallbackInfo ci) {
		if (EarlyLoadingPlatformService.INSTANCE.isRunningInIde() && channel.pipeline().get("timeout") != null) {
			channel.pipeline().remove("timeout");
		}
	}
}
