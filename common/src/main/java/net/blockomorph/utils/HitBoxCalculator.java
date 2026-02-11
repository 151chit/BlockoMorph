package net.blockomorph.utils;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HitBoxCalculator {
	private InPlayerBlockPos minPos = InPlayerBlockPos.get(0, 0, 0);
	private InPlayerBlockPos maxPos = InPlayerBlockPos.get(0, 0, 0);
	private final PlayerAccessor pl;

	public HitBoxCalculator(PlayerAccessor owner) {
		this.pl = owner;
	}

	public InPlayerBlockPos getMinPos() {
		return this.minPos;
	}

	public InPlayerBlockPos getMaxPos() {
		return this.maxPos;
	}

	public void recalculatePositions() {
		this.minPos = this.findMinPos();
		this.maxPos = this.findMaxPos();
	}

	private AABB centerAABB(AABB original, Vec3 center) {
		double width = original.maxX - original.minX;
		double height = original.maxY - original.minY;
		double depth = original.maxZ - original.minZ;

		return new AABB(
				center.x - (width / 2),
				center.y,
				center.z - (depth / 2),
				center.x + (width / 2),
				center.y + height,
				center.z + (depth / 2)
		);
	}

	private InPlayerBlockPos findMinPos() {
		if (this.pl.getBlocksData2().isEmpty())
			return InPlayerBlockPos.ZERO;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		for (InPlayerBlockPos pos : this.pl.getBlocksData2().keySet()) {
			if (pos.getX() < minX) {
				minX = pos.getX();
			}
			if (pos.getY() < minY) {
				minY = pos.getY();
			}
			if (pos.getZ() < minZ) {
				minZ = pos.getZ();
			}
		}
		return InPlayerBlockPos.get(minX, minY, minZ);
	}

	private InPlayerBlockPos findMaxPos() {
		if (this.pl.getBlocksData2().isEmpty())
			return InPlayerBlockPos.get(1, 1, 1);
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (InPlayerBlockPos pos : this.pl.getBlocksData2().keySet()) {
			if (pos.getX() > maxX) {
				maxX = pos.getX();
			}
			if (pos.getY() > maxY) {
				maxY = pos.getY();
			}
			if (pos.getZ() > maxZ) {
				maxZ = pos.getZ();
			}
		}
		return InPlayerBlockPos.get(maxX, maxY, maxZ).offset(1, 1, 1);
	}

	public EntityDimensions calculateDimensions() {
		return new EntityDimensions(0, 0, false) {
			public AABB makeBoundingBox(double d, double e, double f) {
				Vec3 vec3 = new Vec3(d, e, f);
				AABB ab = new AABB(
						vec3.x + minPos.getX(),
						vec3.y + minPos.getY(),
						vec3.z + minPos.getZ(),
						vec3.x + maxPos.getX(),
						vec3.y + maxPos.getY(),
						vec3.z + maxPos.getZ());
				return centerAABB(ab, vec3);
			}
		};
	}

	public float getEyeHeight() {
		return (this.maxPos.getY() - 1) + 0.83300006f + (pl.player().isCrouching() ? -0.125F : 0);
	}
}
