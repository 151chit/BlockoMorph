package net.blockomorph.network;

import net.blockomorph.core.PlayerAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record ClientBoundSerializeInfoPacket(boolean serialize, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_serialize_info_packet";
	private static boolean READY;

	ClientBoundSerializeInfoPacket(FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readVarInt());
	}

	public ClientBoundSerializeInfoPacket(boolean serialize, ServerPlayer player) {
		this(serialize, player.getId());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.serialize);
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (this.clientPlayerById(this.ownerId) instanceof PlayerAccessor pl) {
			READY = true;
			pl.getManager().getFlags().serializingProcess.setValue(this.serialize);
			READY = false;
		}
	}

	public static boolean isReady() {
		return READY;
	}
}
