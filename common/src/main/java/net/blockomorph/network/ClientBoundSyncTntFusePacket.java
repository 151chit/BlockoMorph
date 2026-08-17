package net.blockomorph.network;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientBoundSyncTntFusePacket(int fuse, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_sync_tnt_fuse_packet";

	public ClientBoundSyncTntFusePacket(int fuse, InPlayerManager manager) {
		this(fuse, manager.getOwner().player().getId());
	}

	ClientBoundSyncTntFusePacket(FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.fuse);
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (this.clientPlayerById(this.ownerId) instanceof PlayerAccessor pl) {
			pl.getManager().getTntHandler().setFuse(this.fuse);
		}
	}
}
