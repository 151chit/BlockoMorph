package net.blockomorph.core.render.blockGetter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class EmptyBlockAndTintGetter implements BlockAndTintGetter {
	public static final BlockAndTintGetter INSTANCE = new EmptyBlockAndTintGetter();
	private EmptyBlockAndTintGetter() {}

	@Override
	public float getShade(Direction direction, boolean bl) {
		return 0;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return LevelLightEngine.EMPTY;
	}

	@Override
	public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
		return 0;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
		return null;
	}

	@Override
	public BlockState getBlockState(@Nullable BlockPos blockPos) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public FluidState getFluidState(BlockPos blockPos) {
		return this.getBlockState(null).getFluidState();
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
