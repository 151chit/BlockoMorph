package net.blockomorph.core;

import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class HitBoxCalculator {
	private InPlayerBlockPos minPos = InPlayerBlockPos.get(0, 0, 0);
	private InPlayerBlockPos maxPos = InPlayerBlockPos.get(1, 1, 1);
	private final int[] axisCountX = new int[BlocksInPlayerStorage.ONE_AXIS];
	private final int[] axisCountY = new int[BlocksInPlayerStorage.ONE_AXIS];
	private final int[] axisCountZ = new int[BlocksInPlayerStorage.ONE_AXIS];
	private double partWidth, height, partDepth;
	private int minX, minY, minZ;
	private int maxX, maxY, maxZ;
	private EntityDimensions currentBox = EntityDimensions.fixed(1, 1);
	private final InPlayerManager manager;
	private boolean hitboxChangeEnqueue = true;

	protected HitBoxCalculator(InPlayerManager owner) {
		this.manager = owner.assertOnInit();
		this.minX = this.minZ = this.minY = Integer.MAX_VALUE;
		this.maxX = this.maxZ = this.maxY = Integer.MIN_VALUE;
	}

	public record HitboxData(Vec3 position, InPlayerBlockPos minPos, InPlayerBlockPos maxPos) {}

	public InPlayerBlockPos getMinPos() {
		return this.minPos;
	}

	public InPlayerBlockPos getMaxPos() {
		return this.maxPos;
	}

	public EntityDimensions getDimensions() {
		return this.currentBox;
	}

	public void enqueueHitboxUpdate() {
		this.hitboxChangeEnqueue = true;
	}

	protected void tick() {
		if (this.hitboxChangeEnqueue) {
			this.hitboxChangeEnqueue = false;
			this.manager.getOwner().player().refreshDimensions();
		}
	}

	public void refreshPositions() {
		this.minPos = this.size() == 0 ? InPlayerBlockPos.ZERO : InPlayerBlockPos.get(this.minX - 16, this.minY - 1, this.minZ - 16);
		this.maxPos = this.size() == 0 ? InPlayerBlockPos.ONE : InPlayerBlockPos.get(this.maxX - 15, this.maxY, this.maxZ - 15);
		this.currentBox = ManualHitboxEntityDimension.dynamic(this::makeAABB, this.getEyeHeight());
		this.partWidth = (double) (this.maxPos.getX() - this.minPos.getX()) /2;
		this.height = this.maxPos.getY() - this.minPos.getY();
		this.partDepth = (double) (this.maxPos.getZ() - this.minPos.getZ()) /2;
	}

	private int size() {
		return this.manager.getBlocksStorage().size();
	}

	private AABB makeAABB(double x, double y, double z) {
		return new AABB(x - this.partWidth, y, z - this.partDepth, x + this.partWidth, y + this.height, z + this.partDepth);
	}

	protected void onBlockAdded(BlockInPlayer2 block) {
		this.checkAxis(block, false);
		this.applyArrayOffset(block, (x, y, z) -> {
			this.minX = Math.min(this.minX, x);
			this.minY = Math.min(this.minY, y);
			this.minZ = Math.min(this.minZ, z);
			this.maxX = Math.max(this.maxX, x);
			this.maxY = Math.max(this.maxY, y);
			this.maxZ = Math.max(this.maxZ, z);
		});
	}

	protected void onBlockRemoved(BlockInPlayer2 block) {
		if (this.size() == 0) {
			this.onAllBlocksRemoved();
			return;
		}
		this.checkAxis(block, true);
		this.applyArrayOffset(block, (x, y, z) -> {
			if (x == this.minX) {
				while (this.axisCountX[this.minX] == 0) this.minX++;
			}
			if (x == this.maxX) {
				while (this.axisCountX[this.maxX] == 0) this.maxX--;
			}

			if (y == this.minY) {
				while (this.axisCountY[this.minY] == 0) this.minY++;
			}
			if (y == this.maxY) {
				while (this.axisCountY[this.maxY] == 0) this.maxY--;
			}

			if (z == this.minZ) {
				while (this.axisCountZ[this.minZ] == 0) this.minZ++;
			}
			if (z == this.maxZ) {
				while (this.axisCountZ[this.maxZ] == 0) this.maxZ--;
			}
		});
	}

	protected void onAllBlocksRemoved() {
		Arrays.fill(this.axisCountX, 0);
		Arrays.fill(this.axisCountY, 0);
		Arrays.fill(this.axisCountZ, 0);
		this.minX = this.minZ = this.minY = Integer.MAX_VALUE;
		this.maxX = this.maxZ = this.maxY = Integer.MIN_VALUE;
	}

	private void checkAxis(BlockInPlayer2 block, boolean remove) {
		if (remove) {
			this.applyArrayOffset(block, (x, y, z) -> {
				this.axisCountX[x]--;
				this.axisCountY[y]--;
				this.axisCountZ[z]--;
			});
		} else {
			this.applyArrayOffset(block, (x, y, z) -> {
				this.axisCountX[x]++;
				this.axisCountY[y]++;
				this.axisCountZ[z]++;
			});
		}
	}

	private void applyArrayOffset(BlockInPlayer2 block, OffsetPosConsumer consumer) {
		InPlayerBlockPos pos = block.getOffset();
		int x = pos.getX() + 16;
		int y = pos.getY() + 1;
		int z = pos.getZ() + 16;
		consumer.accept(x, y, z);
	}

	public float getEyeHeight() {
		return (this.maxPos.getY() - 1) + 0.83300006f;
	}

	@FunctionalInterface
	private interface OffsetPosConsumer {
		void accept(int x, int y, int z);
	}

	@FunctionalInterface
	public interface BlockHitBox {
		AABB makeBox(double x, double y, double z);
	}

	@FunctionalInterface
	public interface ManualHitboxEntityDimension {
		void overrideCreation(HitBoxCalculator.BlockHitBox hitBox);

		static EntityDimensions dynamic(HitBoxCalculator.BlockHitBox hitBox, float eye) {
			EntityDimensions dummy = EntityDimensions.fixed(0, 0).withEyeHeight(eye);
			((ManualHitboxEntityDimension) (Object) dummy).overrideCreation(hitBox);
			return dummy;
		}
	}
}
