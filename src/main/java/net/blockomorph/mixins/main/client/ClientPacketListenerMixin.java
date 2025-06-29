package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	@Inject(method = "handlePlayerCombatKill", at = @At("HEAD"), cancellable = true)
	public void autoRespawn(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		Entity entity = player.level().getEntity(packet.playerId());
		if (entity == player && PlayerAccessor.of(player).isActive()) {
			ci.cancel();
			player.respawn();
		}
	}

	@Redirect(method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private void redirectSetOverlayMessage(Gui instance, Component component, boolean bl) {
		if (this.needEjectMessage()) {
			instance.setOverlayMessage(component, bl);
		}
	}

	@Redirect(method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;sayNow(Lnet/minecraft/network/chat/Component;)V"))
	private void redirectSayNow(GameNarrator instance, Component component) {
		if (this.needEjectMessage()) {
			instance.sayNow(component);
		}
	}

	private boolean needEjectMessage() {
		LocalPlayer pl = Minecraft.getInstance().player;
		if (pl != null && pl.getVehicle() instanceof PlayerAccessor playerAccessor) {
			return !playerAccessor.isFullActive();
		}
		return true;
	}

}
