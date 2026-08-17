package net.blockomorph.mixins.main.system.morphState.playerFeatures;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.Config;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
	private void checkAutoRespawn(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
		LocalPlayer player = GuiUtils.MC.player;
		if (player != null && player.getId() == packet.playerId()) {
			if (PlayerAccessor.of(player).isBlockomorphActive()) {
				player.setShowDeathScreen(!Config.get().autoRespawn.getValue());
			}
		}
	}
}
