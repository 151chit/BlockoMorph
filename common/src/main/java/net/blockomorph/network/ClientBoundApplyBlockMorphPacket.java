package net.blockomorph.network;

import net.blockomorph.core.PlayerAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record ClientBoundApplyBlockMorphPacket(BlockState state, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_apply_block_morph_packet";

	public ClientBoundApplyBlockMorphPacket(BlockState state, PlayerAccessor player) {
		this(state, player.player().getId());
	}

	ClientBoundApplyBlockMorphPacket(FriendlyByteBuf buf) {
		this(buf.readById(Block::stateById), buf.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeById(Block::getId, this.state);
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (this.clientPlayerById(this.ownerId) instanceof PlayerAccessor pl)
			pl.getManager().receiveClientBlockMorph(this.state);
	}
}
