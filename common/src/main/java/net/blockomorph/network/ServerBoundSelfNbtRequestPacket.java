package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ServerBoundSelfNbtRequestPacket implements BlockMorphPacket {
	public static final String ID = "server_bound_self_nbt_request_packet";

	public ServerBoundSelfNbtRequestPacket(FriendlyByteBuf friendlyByteBuf) {
	}

	public ServerBoundSelfNbtRequestPacket() {
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (player instanceof PlayerAccessor pl && pl.isFullActive() && pl instanceof ServerPlayer serverPlayer) {
			CompoundTag tag = pl.getTag(InPlayerBlockPos.ZERO);
			MorphUtils.sendPlayer(ClientBoundServerBlockEntityTagPacket.createForTag(tag), serverPlayer);
		}
	}
}
