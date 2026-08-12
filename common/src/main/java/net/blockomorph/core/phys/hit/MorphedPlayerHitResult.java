package net.blockomorph.core.phys.hit;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class MorphedPlayerHitResult extends BlockHitResult {//todo: mixins getblock()
	private final BlockInPlayer2 block;

	protected MorphedPlayerHitResult(BlockInPlayer2 block, Vec3 absoluteHit, Direction direction, boolean inside) {
		super(calculateKeyPoint(block, absoluteHit), direction, block.getPos(), inside);
		this.block = block;
	}

	private MorphedPlayerHitResult(BlockInPlayer2 block, Vec3 position, Direction direction, boolean inside, boolean border) {
		super(position, direction, block.getPos(), inside, border);
		this.block = block;
	}

	private static Vec3 calculateKeyPoint(BlockInPlayer2 block, Vec3 absoluteHit) {
		double realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
		double realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
		double realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
		return new Vec3(
				absoluteHit.x - realX + block.getPos().getX(),
				absoluteHit.y - realY + block.getPos().getY(),
				absoluteHit.z - realZ + block.getPos().getZ());
	}

	public BlockInPlayer2 getBlock() {
		return this.block;
	}

	public double distanceToRealSqr(Vec3 fromReal) {
		Vec3 point = this.getLocation();
		double realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, this.block);
		double realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, this.block);
		double realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, this.block);
		double absoluteX = point.x - this.block.getPos().getX() + realX;
		double absoluteY = point.y - this.block.getPos().getY() + realY;
		double absoluteZ = point.z - this.block.getPos().getZ() + realZ;
		return fromReal.distanceToSqr(absoluteX, absoluteY, absoluteZ);
	}

	public Vec3 getRealPos() {
		Vec3 point = this.getLocation();
		double realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, this.block);
		double realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, this.block);
		double realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, this.block);
		return new Vec3(point.x - this.block.getPos().getX() + realX, point.y - this.block.getPos().getY() + realY, point.z - this.block.getPos().getZ() + realZ);
	}

	@Override
	public BlockHitResult withDirection(Direction direction) {
		return new MorphedPlayerHitResult(this.block, this.getLocation(), direction, this.isInside(), this.isWorldBorderHit());
	}

	@Override
	public BlockHitResult withPosition(BlockPos blockPos) {
		if (InPlayerBlockPos.isInvalidPosFor(this.block.getOwner(), blockPos))
			return this.broken(blockPos);
		int offset = InPlayerBlockPos.fromDelta(this.block.getOwner().getZeroKey(), blockPos);
		BlockInPlayer2 block = this.block.getOwner().getBlock(offset);
		if (block == null)
			return this.broken(blockPos);
		return new MorphedPlayerHitResult(block, this.getLocation(), this.getDirection(), this.isInside(), this.isWorldBorderHit());
	}

	private BlockHitResult broken(BlockPos pos) {
		return BlockHitResult.miss(Vec3.atLowerCornerOf(pos), this.getDirection(), pos);
	}

	@Override
	public BlockHitResult hitBorder() {
		return new MorphedPlayerHitResult(this.block, this.getLocation(), this.getDirection(), this.isInside(), true);
	}
}
