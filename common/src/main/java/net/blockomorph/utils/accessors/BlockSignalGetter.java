package net.blockomorph.utils.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockSignalGetter {
	int getSignal(BlockState blockState, BlockGetter level, BlockPos pos, Direction direction);
}