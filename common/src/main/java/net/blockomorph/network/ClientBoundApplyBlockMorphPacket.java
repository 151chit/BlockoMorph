package net.blockomorph.network;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ClientBoundApplyBlockMorphPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_apply_block_morph_packet";

	BlockState state;
	int id;

	public ClientBoundApplyBlockMorphPacket(BlockState state, PlayerAccessor player) {
		this.state = state;
		this.id = player.player().getId();
	}

	public ClientBoundApplyBlockMorphPacket(FriendlyByteBuf buf) {
		this.state = buf.readById(Block.BLOCK_STATE_REGISTRY);
		this.id = buf.readInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeId(Block.BLOCK_STATE_REGISTRY, this.state);
		buffer.writeInt(this.id);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		Entity ent = Minecraft.getInstance().level.getEntity(this.id);
		if (ent instanceof PlayerAccessor pl) {
			pl.setOnLoadingBlocks(true);
			for (InPlayerBlockPos pos : pl.getBlocksData2().keySet()) {
				if (!pos.equals(InPlayerBlockPos.ZERO)) {
					pl.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
				}
			}
			pl.setBlockState(InPlayerBlockPos.ZERO, this.state, false);
			BlockPos pos = InPlayerBlockPos.ZERO.boundedBlockPos(pl.player());
			if (pos != null) {
				state.getBlock().setPlacedBy(pl.player().level(), pos, state, pl.player(), new ItemStack(this.state.getBlock().asItem(), 1));
			}
			pl.setOnLoadingBlocks(false);
		}
	}
}
