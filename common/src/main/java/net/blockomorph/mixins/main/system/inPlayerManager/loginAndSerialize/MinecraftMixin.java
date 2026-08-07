package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
	public void releaseLevel(ClientLevel level, boolean stopSound, CallbackInfo ci) {
		if (level == null) BlockPosBounds.releaseClientData();
	}
}