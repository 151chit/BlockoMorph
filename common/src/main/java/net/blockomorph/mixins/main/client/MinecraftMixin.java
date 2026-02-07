package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.SideSelector;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Shadow
	@Nullable
	public ClientLevel level;

	@Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
	public void releaseLevel(ClientLevel clientLevel, boolean bl, CallbackInfo ci) {
		if (clientLevel == null) {
			BlockPosBounds.clearClientCache();
		}
	}

	@Inject(require = 1, method = "<init>", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;<init>(Ljava/lang/String;)V", unsafe = true))
	private void run(GameConfig p_91084_, CallbackInfo ci) {
		SideSelector.markClient();
	}
}