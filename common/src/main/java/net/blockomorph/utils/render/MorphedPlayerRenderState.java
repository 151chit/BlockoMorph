package net.blockomorph.utils.render;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public final class MorphedPlayerRenderState {
	public float deltaTick;
	public BlockPos sleepingPos;
	public MorphedState morphedState;

	public sealed interface MorphedState permits TntMorphedState, BlockMorphedState {}

	public static final class TntMorphedState implements MorphedState {
		TntRenderState tntRenderState;
	}

	public static final class BlockMorphedState implements MorphedState {
		BlockAndTintGetter level;
		Vector2d matrixOffset;
		@Nullable VoxelShape framedBlock;
		List<BlockInfo> blocks = new ArrayList<>();
	}

	public static class BlockInfo {
		InPlayerBlockPos offset;
		BlockPos keyPos;
		BlockState blockState;
		@Nullable BlockEntity blockEntity;
		boolean needFluidAction;
		int brakeProgress;
		int renderLight;
	}

	public boolean isBlock() {
		return this.morphedState instanceof BlockMorphedState;
	}
}
