package net.blockomorph.mixins.main.system.inPlayerManager.render.baker;

import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(at = @At("HEAD"), method = "updateLevelInEngines")
	private void release(ClientLevel clientLevel, CallbackInfo ci) {
		if (clientLevel == null) {
			PlayersAsyncBakersManager.Provider.getFromVanilla().clearAllPlayers();
		}
	}
}
