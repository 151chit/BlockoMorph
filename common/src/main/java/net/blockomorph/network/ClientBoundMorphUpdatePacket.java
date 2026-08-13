package net.blockomorph.network;

import io.netty.handler.codec.DecoderException;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.serialization.BlockPalette;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public record ClientBoundMorphUpdatePacket(BlockPalette palette, int ownerId) implements BlockMorphPacket {
	public static final String ID = "client_bound_morph_update_packet";

	public ClientBoundMorphUpdatePacket(BlockPalette palette, PlayerAccessor pl) {
		this(palette, pl.player().getId());
	}

	ClientBoundMorphUpdatePacket(FriendlyByteBuf buffer) {
		this(new BlockPalette(buffer.readCollection(i -> {
			if (i > BlocksInPlayerStorage.SIZE) throw new DecoderException("Palette ids to large: " + i);
			return new ArrayList<>(i);
		}, (buf) -> Block.stateById(buf.readVarInt())),
			buffer.readVarIntArray(BlocksInPlayerStorage.SIZE),
			buffer.readVarIntArray(BlocksInPlayerStorage.SIZE)),
		buffer.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeCollection(this.palette.ids(), (buf, state) -> buf.writeVarInt(Block.getId(state)));
		buffer.writeVarIntArray(this.palette.poses());
		buffer.writeVarIntArray(this.palette.types());
		buffer.writeVarInt(this.ownerId);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (this.clientPlayerById(this.ownerId) instanceof PlayerAccessor pl) {
			if (this.palette.poses().length != this.palette.types().length)
				throw new IllegalArgumentException("other length");
			for (int i = 0; i < this.palette.poses().length; i++) {
				int pos = this.palette.poses()[i];
				int type = this.palette.types()[i];
				if (!InPlayerBlockPos.isValid(pos))
					throw new IllegalArgumentException("pos invalid");
				if (type < 0 || type >= this.palette.ids().size())
					throw new IllegalArgumentException("type invalid");
				BlockState blockState = this.palette.ids().get(type);
				pl.getManager().setBlock(pos, blockState, 19, 2);
			}
			pl.getManager().getHitBoxCalculator().refreshPositions();
			pl.player().refreshDimensions();
		}
	}
}
