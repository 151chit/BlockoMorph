package net.blockomorph.network;

import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ServerBoundBlockMorphPacket implements BlockMorphPacket {
	public static final String ID = "server_bound_block_morph_packet";
	private final Tag tag;

	private ServerBoundBlockMorphPacket(CompoundTag nbt) {
		this.tag = nbt;
	}

	ServerBoundBlockMorphPacket(FriendlyByteBuf buffer) {
		this.tag = buffer.readNbt(NbtAccounter.defaultQuota());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(this.tag);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (player instanceof PlayerAccessor pl) {
			if (this.tag instanceof CompoundTag morphTag) {
				BlockState blockstate = Block.stateById(morphTag.getInt("s").orElse(-1));
				CompoundTag nbt = morphTag.getCompound("t").orElse(null);
				this.doMorph(pl, blockstate, nbt);
			} else {
				pl.getManager().getTntHandler().tryActivateDirect();
			}
		}
	}

	private void doMorph(PlayerAccessor pl, BlockState state, CompoundTag nbt) {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(pl.player());
		BlockState currentState = pl.getBlockState(InPlayerBlockPos.ZERO);
		if (!access.morph) {
			if (!currentState.is(state.getBlock())) {
				throw new IllegalArgumentException("You not have access to change your blockstate.");
			}
		}
		if (!access.config) {
			if (nbt != null || state != state.getBlock().defaultBlockState()) {
				throw new IllegalArgumentException("You not have access to config your block.");
			}
		}
		BannedBlock reason = pl.applyBlockMorph(state, nbt, BannedBlock.Source.NETWORK);
		if (reason != null && reason != BannedBlock.ALREADY_MORPHED) {
			throw new IllegalArgumentException(reason.reason());
		}
	}

	public static ServerBoundBlockMorphPacket create(BlockState state, @Nullable CompoundTag tagMorph) {
		CompoundTag root = new CompoundTag();
		root.putInt("s", Block.getId(state));
		if (tagMorph != null) root.put("t", tagMorph);
		return new ServerBoundBlockMorphPacket(root);
	}

	public static ServerBoundBlockMorphPacket fuseTnt() {
		return new ServerBoundBlockMorphPacket((CompoundTag) null);
	}
}
