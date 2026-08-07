package net.blockomorph.network;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ServerBoundSelfNbtRequestPacket implements BlockMorphPacket {
	public static final ServerBoundSelfNbtRequestPacket INSTANCE = new ServerBoundSelfNbtRequestPacket();
	public static final String ID = "server_bound_self_nbt_request_packet";
	private ServerBoundSelfNbtRequestPacket() {}
	@Override public void write(FriendlyByteBuf buffer) {}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (player instanceof PlayerAccessor pl && pl.isBlockomorphFullActive() && pl instanceof ServerPlayer serverPlayer) {
			CompoundTag tag = pl.getTag(InPlayerBlockPos.ZERO);
			if (tag == null) tag = new CompoundTag();
			serverPlayer.connection.send(ClientBoundServerBlockEntityTagPacket.createForTag(tag).toVanillaServerBound());
		}
	}
}
