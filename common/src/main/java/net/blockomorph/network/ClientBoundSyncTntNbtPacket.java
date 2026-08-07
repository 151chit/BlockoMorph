package net.blockomorph.network;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientBoundSyncTntNbtPacket(CompoundTag tag, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_sync_tnt_nbt_packet";

	public ClientBoundSyncTntNbtPacket(CompoundTag tag, InPlayerManager manager) {
		this(tag, manager.getOwner().player().getId());
	}

	ClientBoundSyncTntNbtPacket(FriendlyByteBuf buffer) {
		this(buffer.readNbt(), buffer.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(this.tag);
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (this.clientPlayerById(this.ownerId) instanceof PlayerAccessor pl) {
			pl.getManager().getTntHandler().loadFullTag(this.tag);
		}
	}
}
