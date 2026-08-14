package net.blockomorph.core.storage;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class BlocksInPlayerCursor3D implements AutoCloseable {
	private final ReusableIterator iterator = new ReusableIterator();
	private final InPlayerManager manager;

	public BlocksInPlayerCursor3D(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
	}

	public Iterable<BlockInPlayer2> forAllBlocks(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		Side.assertOnGameThread(this.manager);
		if (this.iterator.processing) {
			ReusableIterator fallback = new ReusableIterator();
			fallback.prepare(minX, minY, minZ, maxX, maxY, maxZ);
			MorphUtils.LOGGER.warn("Recursive block 3D iterating in playerOwner: {}", this.manager, new Throwable());
			return fallback;
		}
		this.iterator.prepare(minX, minY, minZ, maxX, maxY, maxZ);
		return this.iterator;
	}

	public Iterable<BlockInPlayer2> forAllBlocks(AABB aabb) {
		return this.forAllBlocks(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}

	@Override
	public void close() {
		this.iterator.close();
	}

	private class ReusableIterator implements Iterator<BlockInPlayer2>, Iterable<BlockInPlayer2>, AutoCloseable {
		private int minX, minZ, maxX, maxY, maxZ, x, y, z;
		private double realX, realY, realZ;
		private boolean processing;
		private boolean end;
		private BlockInPlayer2 nextBlock;

		private void prepare(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			this.realX = MorphMath.getRealBlockPosCenter(Direction.Axis.X, manager.getOwner());
			this.realY = MorphMath.getRealBlockPosCenter(Direction.Axis.Y, manager.getOwner());
			this.realZ = MorphMath.getRealBlockPosCenter(Direction.Axis.Z, manager.getOwner());
			this.minX = this.x = this.clampBounds(Mth.floor(Math.min(minX, maxX) - this.realX), true);
						this.y = this.clampBounds(Mth.floor(Math.min(minY, maxY) - this.realY), false);
			this.minZ = this.z = this.clampBounds(Mth.floor(Math.min(minZ, maxZ) - this.realZ), true);
			this.maxX = this.clampBounds(Mth.floor(Math.max(minX, maxX) - this.realX), true);
			this.maxY = this.clampBounds(Mth.floor(Math.max(minY, maxY) - this.realY), false);
			this.maxZ = this.clampBounds(Mth.floor(Math.max(minZ, maxZ)- this.realZ), true);

			this.processing = true;
		}

		private int clampBounds(int i, boolean horizontal) {
			if (horizontal) return Mth.clamp(i, -14, 14);
			return Mth.clamp(i, 0, 30);
		}

		private void checkMods() {
			double realX = MorphMath.getRealBlockPosCenter(Direction.Axis.X, manager.getOwner());
			double realY = MorphMath.getRealBlockPosCenter(Direction.Axis.Y, manager.getOwner());
			double realZ = MorphMath.getRealBlockPosCenter(Direction.Axis.Z, manager.getOwner());
			if (realX != this.realX || realY != this.realY || realZ != this.realZ)
				throw new ConcurrentModificationException();
		}

		private boolean advance() {
			this.x++;
			if (this.x > this.maxX) {
				this.x = this.minX;
				this.z++;
				if (this.z > this.maxZ) {
					this.z = this.minZ;
					this.y++;
					return this.y <= this.maxY;
				}
			}
			return true;
		}

		@Override
		public boolean hasNext() {
			this.checkMods();
			if (manager.getBlocksStorage().size() == 0) return false;
			if (this.end) return false;
			if (this.nextBlock != null) return true;
			while (true) {
				this.nextBlock = manager.getBlocksStorage().get(InPlayerBlockPos.asInt(this.x, this.y, this.z));
				if (!this.advance()) {
					this.end = true;
					return this.nextBlock != null;
				}
				if (this.nextBlock != null) {
					return true;
				}
			}
		}

		@Override
		public BlockInPlayer2 next() {
			if (this.nextBlock != null) {
				BlockInPlayer2 block = this.nextBlock;
				this.nextBlock = null;
				return block;
			}
			throw new UnsupportedOperationException("Call hasNext before!");
		}

		@Override
		public Iterator<BlockInPlayer2> iterator() {
			return this;
		}

		@Override
		public void close() {
			this.processing = false;
			this.nextBlock = null;
			this.end = false;
		}
	}
}
