package net.blockomorph.mixins.main.register.network;

import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientListener implements ClientCommonPacketListener {
	@Shadow @Final protected Minecraft minecraft;
	@Shadow @Final protected Connection connection;
	@Unique private String error;

	@FastInject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"))
	private boolean handle(ClientboundCustomPayloadPacket packet) {
		if (this.isClientGame() && packet.payload() instanceof MorphNetwork.BlockomorphCustomPayload payload) {
			PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
			String err = MorphNetwork.handleCustomPayload(payload, PacketFlow.CLIENTBOUND, this.minecraft.player);
			if (err != null) {
				MorphUtils.LOGGER.error("Error packet data from server: {}", err);
				this.minecraft.disconnect(this.makeDisconnectScreen(
						"Blockomorph mod send broken data from server", err
				), false, false);
			}
			return false;
		} else if (packet.payload() instanceof MorphNetwork.ErrorMarkPayload(String err)) {
			this.error = err;
			return false;
		}
		return true;
	}

	@FastInject(method = "createDisconnectScreen", at = @At("HEAD"))
	private Object handleErr(DisconnectionDetails details) {
		if (this.error != null)
			return this.makeDisconnectScreen("Blockomorph mod on your client send broken data", this.error);
		return FastInject.CONTINUE_EXECUTION;
	}

	@FastInject(method = "send", at = @At("HEAD"))
	private boolean bypassNeoforge(Packet<?> packet) {
		if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload) && payload instanceof MorphNetwork.MorphPayload) {
			this.connection.send(packet);
			return false;
		}
		return true;
	}

	@Unique
	private Screen makeDisconnectScreen(String header, String error) {
		return new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), Component.literal(header), Component.literal(error));
	}

	@Unique
	protected boolean isClientGame() {
		return (Object)this instanceof ClientPacketListener;
	}
}
