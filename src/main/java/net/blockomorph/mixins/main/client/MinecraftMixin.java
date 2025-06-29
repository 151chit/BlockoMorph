package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.coords.BlockPosBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Shadow @Nullable public ClientLevel level;

	@Inject(method = "setLevel", at = @At("HEAD"))
	public void releaseLevel(ClientLevel clientLevel, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
		if (this.level != null) {
			BlockPosBounds.onLevelUnload(this.level);
		}
	}
}
