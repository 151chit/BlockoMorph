package net.blockomorph.core.phys;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GapMovementCorrector {
	private static final double EPSILON = 1.0E-5D;

	public static boolean tryEnterInGap(Player player, Vec3 deltaMovementRaw) {
		if (deltaMovementRaw.lengthSqr() < 1.0E-7D) return false;

		Direction pressureDir = Direction.getApproximateNearest(deltaMovementRaw);

		Direction.Axis gapAxis = pressureDir.getAxis();

		Direction.Axis plateAxis1 = gapAxis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
		double offset1 = deltaMovementRaw.get(plateAxis1);
		Direction.AxisDirection dir1 = offset1 < 0 ? Direction.AxisDirection.NEGATIVE : (offset1 > 0 ? Direction.AxisDirection.POSITIVE : null);
		if (dir1 == null) return false;

		Direction.Axis plateAxis2 = gapAxis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
		double offset2 = deltaMovementRaw.get(plateAxis2);
		Direction.AxisDirection dir2 = offset2 < 0 ? Direction.AxisDirection.NEGATIVE : (offset2 > 0 ? Direction.AxisDirection.POSITIVE : null);
		if (dir2 == null) return false;

		AABB hitbox = player.getBoundingBox();
		AABB scanPlate = createScanPlate(hitbox, pressureDir);
		Iterable<VoxelShape> collisions = player.level().getCollisions(player, scanPlate);
		if (!collisions.iterator().hasNext()) return false;

		DoubleList axis1Shifts = scanPerAxis(hitbox, collisions, plateAxis1, dir1);
		DoubleList axis2Shifts = scanPerAxis(hitbox, collisions, plateAxis2, dir2);
		for (double pos1 : axis1Shifts) {
			for (double pos2 : axis2Shifts) {
				AABB alignedBox = hitbox;
				alignedBox = moveByAxis(alignedBox, plateAxis1, pos1);
				alignedBox = moveByAxis(alignedBox, plateAxis2, pos2);
				alignedBox = alignedBox.move(pressureDir.getUnitVec3().scale(EPSILON));
				if (player.level().noCollision(player, alignedBox)) {
					Vec3 newPos = alignedBox.getBottomCenter();
					player.setPos(newPos);
					return true;
				}
			}
		}
		return false;
	}

	private static DoubleList scanPerAxis(AABB hitbox, Iterable<VoxelShape> collisions, Direction.Axis plateAxis, Direction.AxisDirection way) {
		DoubleList list = new DoubleArrayList();
		list.add(0);
		double hitboxPos = way == Direction.AxisDirection.POSITIVE ? hitbox.min(plateAxis) : hitbox.max(plateAxis);
		for (VoxelShape shape : collisions) {
			for (AABB box : shape.toAabbs()) {
				double pos = way == Direction.AxisDirection.POSITIVE ? box.max(plateAxis) : box.min(plateAxis);
				if (Math.abs(pos - hitboxPos) < 0.05)
					list.add(pos - hitboxPos);
			}
		}
		return list;
	}

	private static AABB createScanPlate(AABB hitbox, Direction gapAxis) {
		var way = gapAxis.getAxisDirection();
		return switch (gapAxis.getAxis()) {
			case X -> {
				double x = way == Direction.AxisDirection.POSITIVE ? hitbox.maxX : hitbox.minX;
				yield new AABB(x - EPSILON, hitbox.minY, hitbox.minZ,
						x + EPSILON, hitbox.maxY, hitbox.maxZ).move(EPSILON * way.getStep(), 0, 0);
			}
			case Y -> {
				double y = way == Direction.AxisDirection.POSITIVE ? hitbox.maxY : hitbox.minY;
				yield new AABB(hitbox.minX, y - EPSILON, hitbox.minZ,
						hitbox.maxX, y + EPSILON, hitbox.maxZ).move(0, EPSILON * way.getStep(), 0);
			}
			case Z -> {
				double z = way == Direction.AxisDirection.POSITIVE ? hitbox.maxZ : hitbox.minZ;
				yield new AABB(hitbox.minX, hitbox.minY, z - EPSILON,
						hitbox.maxX, hitbox.maxY, z + EPSILON).move(0, 0, EPSILON * way.getStep());
			}
		};
	}

	private static AABB moveByAxis(AABB aabb, Direction.Axis axis, double value) {
		return switch (axis) {
			case X -> aabb.move(value, 0, 0);
			case Y -> aabb.move(0, value, 0);
			case Z -> aabb.move(0, 0, value);
		};
	}
}
