package net.blockomorph.core.render.blockGetter;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public interface InPlayerBlockAndTintGetter extends BlockAndTintGetter {

	void forEachRenderBlocks(Output blockConsumer);
	int renderSize();
	void noExternal(boolean yes);
	boolean isExternalPos(BlockPos pos);
	BlockPos getZeroKeyPos();
	BlockPos.MutableBlockPos blockPosHolder();
	BlockPos realCenterPos();
	Object extractRenderDataFrom(BlockEntity blockEntity);
	BlockAndTintGetter getRealWorld();

	default int getAxisOffset(Direction.Axis axis, BlockPos keyPos) {
		BlockPos zero = this.getZeroKeyPos();
		return axis.choose(keyPos.getX(), keyPos.getY(), keyPos.getZ()) - axis.choose(zero.getX(), zero.getY(), zero.getZ());
	}

	default int translateToOffset(BlockPos pos) {
		return InPlayerBlockPos.fromDelta(this.getZeroKeyPos(), pos);
	}

	default BlockPos translateToReal(BlockPos pos) {
		return this.blockPosHolder().set(
				pos.getX() - this.getZeroKeyPos().getX() + this.realCenterPos().getX(),
				pos.getY() - this.getZeroKeyPos().getY() + this.realCenterPos().getY(),
				pos.getZ() - this.getZeroKeyPos().getZ() + this.realCenterPos().getZ()
		);
	}

	default BlockPos translateToReal(int offset) {
		return this.blockPosHolder().set(
				InPlayerBlockPos.getX(offset) + this.realCenterPos().getX(),
				InPlayerBlockPos.getY(offset) + this.realCenterPos().getY(),
				InPlayerBlockPos.getZ(offset) + this.realCenterPos().getZ()
		);
	}

	static void checkCornerBlocks(int pos, IntPredicate containsCondition, IntConsumer outPut) {
		if (InPlayerBlockPos.isBytesInvalid(pos)) return;
		int centerX = InPlayerBlockPos.getX(pos);
		int centerY = InPlayerBlockPos.getY(pos);
		int centerZ = InPlayerBlockPos.getZ(pos);
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					int newPos = InPlayerBlockPos.asInt(centerX + x, centerY + y, centerZ + z);
					if (newPos != -1 && newPos != pos) {
						if (!containsCondition.test(newPos)) {
							outPut.accept(newPos);
						}
					}
				}
			}
		}
	}

	@Override
	default FluidState getFluidState(BlockPos pos) {
		return this.getBlockState(pos).getFluidState();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	@FunctionalInterface
	interface Output {
		boolean order(BlockPos keyPos, BlockState blockState, boolean renderFluid);
	}
}
