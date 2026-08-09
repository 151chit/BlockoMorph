package net.blockomorph.core;

import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface PlayerBlocksEditor {
	InPlayerManager getManager();
	default boolean isNotInitialized() {
		return this.getManager() == null;
	}

	default BlocksInPlayerStorage getBlocksStorage() {
		if (this.isNotInitialized()) return BlocksInPlayerStorage.BLACKHOLE;
		return this.getManager().getBlocksStorage();
	}

	default int size() {
		return this.getBlocksStorage().size();
	}

	@Nullable
	default BlockInPlayer2 getBlock(int inPlayerBlockPos) {
		return this.getBlocksStorage().get(inPlayerBlockPos);
	}

	default BlockState getBlockState(int inPlayerBlockPos) {
		BlockInPlayer2 block = this.getBlock(inPlayerBlockPos);
		if (block != null) return block.getBlockState();
		return Blocks.AIR.defaultBlockState();
	}

	default int getBiggestDestroyProgress() {
		if (!(this.getManager() instanceof ClientInPlayerManager mn)) return -1;
		return mn.getDestructionHandler().getBestProgress();
	}

	default boolean setBlock(int inPlayerBlockPos, BlockState state, int flags) {
		if (this.isNotInitialized()) return false;
		return this.getManager().setBlock(inPlayerBlockPos, state, flags);
	}

	@Nullable
	default BlockEntity getBlockEntity(int inPlayerBlockPos) {
		BlockInPlayer2 block = this.getBlock(inPlayerBlockPos);
		if (block != null) return block.getBlockEntity();
		return null;
	}
}
