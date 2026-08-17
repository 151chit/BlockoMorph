package net.blockomorph.mixins.main.register.network;

import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGameListener implements ServerGamePacketListener {
	@Shadow public ServerPlayer player;

	@FastInject(method = "handleCustomPayload", at = @At("HEAD"))
	private boolean handle(ServerboundCustomPayloadPacket packet) {
		if (packet.payload() instanceof MorphNetwork.BlockomorphCustomPayload payload) {
			PacketUtils.ensureRunningOnSameThread(packet, this, this.player.serverLevel());
			String err = MorphNetwork.handleCustomPayload(payload, PacketFlow.SERVERBOUND, this.player);
			if (err != null) {
				MorphUtils.LOGGER.error("Error packet data from playerOwner: {} Error: {}", this.player, err);
				this.player.connection.send(new ClientboundCustomPayloadPacket(new MorphNetwork.ErrorMarkPayload(err)));
				this.player.connection.disconnect(Component.literal("Blockomorph packet error"));
			}
			return false;
		}
		return true;
	}
}
