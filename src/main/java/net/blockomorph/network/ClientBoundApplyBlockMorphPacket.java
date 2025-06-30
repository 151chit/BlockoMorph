package net.blockomorph.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class ClientBoundApplyBlockMorphPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_apply_block_morph_packet";

	BlockState state;

	public ClientBoundApplyBlockMorphPacket(BlockState state) {
		this.state = state;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeblock
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {

	}
}
