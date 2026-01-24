package net.blockomorph.network;

import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ServerBoundBlockMorphPacket implements BlockMorphPacket {
	public static final String ID = "server_bound_block_morph_packet";
	CompoundTag tag;

	private ServerBoundBlockMorphPacket(CompoundTag nbt) {
		this.tag = nbt;
	}

	public ServerBoundBlockMorphPacket(FriendlyByteBuf buf) {
		this.tag = buf.readNbt();
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
		if (player instanceof PlayerAccessor mob) {
			if (tag == null) throw new IllegalArgumentException("Payload is null!");
			if (tag.contains("fuse")) {
				mob.setTnt();
				return;
			}
			BlockState blockstate = NbtUtils.readBlockState(player.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
			CompoundTag nbt = tag.contains("Tags", 10) ? tag.getCompound("Tags") : null;
			this.doMorph(mob, blockstate, nbt);
		}
	}

	private void doMorph(PlayerAccessor player, BlockState state, CompoundTag nbt) {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(player.player());
		if (!access.morph) {
			if (!player.getBlockState(InPlayerBlockPos.ZERO).is(state.getBlock())) {
				throw new IllegalArgumentException("You not have access to change your blockstate.");
			}
		}
		if (!access.config) {
			if (nbt != null || !state.getBlock().defaultBlockState().equals(state)) {
				throw new IllegalArgumentException("You not have access to config your block.");
			}
		}
		BannedBlock reason = player.applyBlockMorph(state, nbt, BannedBlock.Source.NETWORK);
		if (reason != null && reason != BannedBlock.ALREADY_MORPHED) {
			throw new IllegalArgumentException(reason.reason());
		}
	}

	public static ServerBoundBlockMorphPacket create(BlockState state, @Nullable CompoundTag tagMorph) {
		CompoundTag tag = new CompoundTag();
		tag.put("BlockState", NbtUtils.writeBlockState(state));
		if (tagMorph != null) tag.put("Tags", tagMorph);
		return new ServerBoundBlockMorphPacket(tag);
	}

	public static ServerBoundBlockMorphPacket fuse() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("fuse", true);
		return new ServerBoundBlockMorphPacket(tag);
	}
}
