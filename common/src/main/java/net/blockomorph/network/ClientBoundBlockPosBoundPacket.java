package net.blockomorph.network;

import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record ClientBoundBlockPosBoundPacket(Long section, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_blockpos_bound_packet";

	ClientBoundBlockPosBoundPacket(FriendlyByteBuf buffer) {
		this(buffer.readNullable(FriendlyByteBuf::readLong), buffer.readVarInt());
	}

	public ClientBoundBlockPosBoundPacket(Player player, Long section) {
		this(section, player.getId());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNullable(this.section, FriendlyByteBuf::writeLong);
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Nullable
	public Player player() {
		return this.clientPlayerById(this.ownerId);
	}

	@Override
	public void handle(Player player) {
		BlockPosBounds.handleBlockPosBound(this);
	}
}
