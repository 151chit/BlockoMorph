package net.blockomorph.mixins.main.register;

import net.blockomorph.screens.morph.TabContentManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class TabBlocksSorterRegister {

	@Inject(method = "adjustPlayer", at = @At("TAIL"))
	private void login(Player player, CallbackInfo ci) {
		TabContentManager.bakeTabs();
	}
}