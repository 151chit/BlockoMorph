package net.blockomorph.mixins.debug;

import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.dedicated.ServerWatchdog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWatchdog.class)
public class WatchDogKillerMixin {

	@FastInject(method = "run", at = @At("HEAD"))
	private boolean remove() {
		return !EarlyLoadingPlatformService.INSTANCE.isRunningInIde();
	}
}
