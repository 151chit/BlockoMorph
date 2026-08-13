package net.blockomorph.core.coords.proxyChunk;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.side.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class ProxyPlayerChunkHandler {

	public static Object setBlockState(PlayerConnectingSource source, BlockPos pos, BlockState state, int flags) {
		InPlayerManager mn = source.managerByPos(pos);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockState old = mn.getBlockState(posIn);
				if (mn.setBlock(posIn, state, flags)) {
					return old;
				}
			}
			return null;
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	public static boolean removeBlockEntity(PlayerConnectingSource source, BlockPos pos) {
		InPlayerManager mn = source.managerByPos(pos);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockInPlayer2 block = mn.getBlock(posIn);
				if (block != null) block.clearBlockEntity();
			}
			return false;
		}
		return true;
	}

	public static boolean changeBlockEntity(PlayerConnectingSource source, BlockEntity blockEntity, boolean changeTicker) {
		BlockPos pos = blockEntity.getBlockPos();
		InPlayerManager mn = source.managerByPos(pos);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockInPlayer2 block = mn.getBlock(posIn);
				if (block != null) block.forceChangeBlockEntity(blockEntity, changeTicker);
			}
			return false;
		}
		return true;
	}

	public static Object getBlockState(PlayerConnectingSource source, Level fallback, BlockPos pos) {
		var blockGetter = LevelWithFlags.of(fallback).flags().overrideBlockGetter;
		if (blockGetter != null && Side.get() != Side.UNKNOWN) {
			return blockGetter.getBlockState(fallback, pos.getX(), pos.getY(), pos.getZ());
		}
		InPlayerManager mn = source.managerByPos(pos);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockState state = mn.getBlockState(posIn);
				if (state.is(Blocks.AIR)) {
					BlockPos realPos = needExternalGet(source, fallback, mn, posIn);
					if (realPos != null) return fallback.getBlockState(realPos);
				}
				return state;
			}
			return Blocks.AIR.defaultBlockState();
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	public static Object getBlockEntity(PlayerConnectingSource source, Level fallback, BlockPos pos) {
		InPlayerManager mn = source.managerByPos(pos);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockEntity be = mn.getBlockEntity(posIn);
				if (be == null) {
					BlockPos realPos = needExternalGet(source, fallback, mn, posIn);
					if (realPos != null) return fallback.getBlockEntity(realPos);
				}
				return be;
			}
			return null;
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	public static Object getFluidState(PlayerConnectingSource source, Level fallback, int x, int y, int z) {
		var blockGetter = LevelWithFlags.of(fallback).flags().overrideBlockGetter;
		if (blockGetter != null && Side.get() != Side.UNKNOWN) {
			return blockGetter.getBlockState(fallback, x, y, z).getFluidState();
		}
		InPlayerManager mn = source.managerByPos(x, z);
		if (mn != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(x, y, z);
			if (posIn != -1) {
				BlockState state = mn.getBlockState(posIn);
				if (state.is(Blocks.AIR)) {
					BlockPos realPos = needExternalGet(source, fallback, mn, posIn);
					if (realPos != null) return fallback.getFluidState(realPos);
				}
				return state.getFluidState();
			}
			return Fluids.EMPTY.defaultFluidState();
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	private static BlockPos needExternalGet(PlayerConnectingSource source, BlockGetter fallback, InPlayerManager mn, int posIn) {
		if (fallback instanceof LevelWithFlags acc && !acc.flags().morphedBlockGetterDisabled) {
			if (posIn != InPlayerBlockPos.ZERO_INT) {
				PlayerAccessor pl = mn.getOwner();
				int x = Mth.floor(MorphMath.getRealBlockPosAxis(Direction.Axis.X, pl, InPlayerBlockPos.getX(posIn)) + 0.5);
				int y = Mth.floor(MorphMath.getRealBlockPosAxis(Direction.Axis.Y, pl, InPlayerBlockPos.getY(posIn)));
				int z = Mth.floor(MorphMath.getRealBlockPosAxis(Direction.Axis.Z, pl, InPlayerBlockPos.getZ(posIn)) + 0.5);
				return source.externalPosHolder().set(x, y, z);
			}
		}
		return null;
	}
}
