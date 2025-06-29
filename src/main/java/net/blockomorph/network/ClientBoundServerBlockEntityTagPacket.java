package net.blockomorph.network;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClientBoundServerBlockEntityTagPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_server_block_entity_packet";

	InPlayerBlockPos pos;
	CompoundTag tg;

	public ClientBoundServerBlockEntityTagPacket(InPlayerBlockPos pos, CompoundTag tg) {
		this.pos = pos;
		this.tg = tg;
	}

	public ClientBoundServerBlockEntityTagPacket(FriendlyByteBuf buffer) {
		this.pos = new InPlayerBlockPos(buffer);
		this.tg = buffer.readNbt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		this.pos.write(buffer);
		buffer.writeNbt(this.tg);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (Minecraft.getInstance().player instanceof PlayerAccessor pl && pl.isFullActive()) {
			BlockInPlayer2 block = pl.getBlocksData2().get(this.pos);
			if (block != null) {
				block.setServerTag(this.tg);
			}
		}
	}
}
