package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Player;

public interface BlockMorphPacket {
	void write(FriendlyByteBuf buffer);

	String getId();

	void handle(Player player);

	default Player clientPlayerById(int ownerId) {
		var level = MorphUtils.Client.getLevel();
		if (level != null && level.getEntity(ownerId) instanceof Player pl) return pl;
		return null;
	}

	default ClientboundCustomPayloadPacket toVanillaClientbound() {
		return new ClientboundCustomPayloadPacket(new MorphNetwork.BlockomorphCustomPayload(this));
	}

	default ServerboundCustomPayloadPacket toVanillaServerBound() {
		return new ServerboundCustomPayloadPacket(new MorphNetwork.BlockomorphCustomPayload(this));
	}
}
