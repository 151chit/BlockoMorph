package net.blockomorph.mixins.main.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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

	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void onOpUpdate(ClientboundEntityEventPacket clientboundEntityEventPacket, CallbackInfo ci, @Local Entity entity) {
		if (entity == GuiUtils.MC.player && GuiUtils.MC.screen instanceof ConfigSyncListener sc) {
			sc.onOperatorRightsChanged();
		}
	}

	@Redirect(method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private void redirectSetOverlayMessage(Gui instance, Component component, boolean bl) {
		if (this.needEjectMessage()) {
			instance.setOverlayMessage(component, bl);
		}
	}

	@Redirect(method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;saySystemNow(Lnet/minecraft/network/chat/Component;)V"))
	private void redirectSayNow(GameNarrator instance, Component component) {
		if (this.needEjectMessage()) {
			instance.saySystemNow(component);
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
