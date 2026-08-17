package net.blockomorph.core.coords.math;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MorphDistanceTo {
	public static double forPoints(double orig,
	                               double fromX, double fromY, double fromZ,
	                               double toX, double toY, double toZ,
	                               boolean sqr, double offset) {
		boolean fromMorphed = InPlayerBlockPos.isMorphedPlayerBlockX(fromX);
		boolean toMorphed = InPlayerBlockPos.isMorphedPlayerBlockX(toX);
		if (fromMorphed || toMorphed) {
			double fromXo = fromX; double fromYo = fromY; double fromZo = fromZ;
			if (fromMorphed) {
				fromXo = MorphNormalizer.normalizeOneOf(fromX, fromY, fromZ, Direction.Axis.X);
				fromYo = MorphNormalizer.normalizeOneOf(fromX, fromY, fromZ, Direction.Axis.Y);
				fromZo = MorphNormalizer.normalizeOneOf(fromX, fromY, fromZ, Direction.Axis.Z);
			}
			double toXo = toX; double toYo = toY; double toZo = toZ;
			if (toMorphed) {
				toXo = MorphNormalizer.normalizeOneOf(toX, toY, toZ, Direction.Axis.X);
				toYo = MorphNormalizer.normalizeOneOf(toX, toY, toZ, Direction.Axis.Y);
				toZo = MorphNormalizer.normalizeOneOf(toX, toY, toZ, Direction.Axis.Z);
			}
			double d0 = fromXo + offset - toXo;
			double d1 = fromYo + offset - toYo;
			double d2 = fromZo + offset - toZo;
			double result = d0 * d0 + d1 * d1 + d2 * d2;
			if (!sqr)
				result = Math.sqrt(result);
			return result;
		}
		return orig;
	}

	public static double forPoints(double orig, Vec3 from, double x, double y, double z, boolean sqr, double offset) {
		return forPoints(orig, from.x, from.y, from.z, x, y, z, sqr, offset);
	}

	public static double forPoints(double orig, Vec3 from, Vec3 to, boolean sqr, double offset) {
		return forPoints(orig, from.x, from.y, from.z, to.x, to.y, to.z, sqr, offset);
	}

	public static double forAabbAndPointSqr(double orig, AABB aabb, Vec3 point) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(point.x) && !InPlayerBlockPos.isMorphedPlayerBlockX(Mth.lerp(0.5, aabb.minX, aabb.maxX))) {
			return orig;
		}
		double dx = getAxisDiff(aabb, MorphNormalizer.normalizeOneOf(point, Direction.Axis.X), Direction.Axis.X);
		double dy = getAxisDiff(aabb, MorphNormalizer.normalizeOneOf(point, Direction.Axis.Y), Direction.Axis.Y);
		double dz = getAxisDiff(aabb, MorphNormalizer.normalizeOneOf(point, Direction.Axis.Z), Direction.Axis.Z);
		return dx * dx + dy * dy + dz * dz;
	}

	@SuppressWarnings("unused")
	public static double forAabbAndAabbSqr(double orig, AABB aabb, AABB aabb2) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(Mth.lerp(0.5, aabb2.minX, aabb2.maxX)) &&
				!InPlayerBlockPos.isMorphedPlayerBlockX(Mth.lerp(0.5, aabb.minX, aabb.maxX))) {
			return orig;
		}
		double dx = getAabbAabbDiff(aabb, aabb2, Direction.Axis.X);
		double dy = getAabbAabbDiff(aabb, aabb2, Direction.Axis.Y);
		double dz = getAabbAabbDiff(aabb, aabb2, Direction.Axis.Z);
		return dx * dx + dy * dy + dz * dz;
	}

	private static double getAxisDiff(AABB aabb, double normCoord, Direction.Axis axis) {
		double min = MorphNormalizer.tryNormalizeAabbAxis(aabb, axis, Direction.AxisDirection.NEGATIVE);
		double max = MorphNormalizer.tryNormalizeAabbAxis(aabb, axis, Direction.AxisDirection.POSITIVE);
		return Math.max(Math.max(min - normCoord, normCoord - max), 0.0);
	}

	private static double getAabbAabbDiff(AABB aabb, AABB aabb2, Direction.Axis axis) {
		double min1 = MorphNormalizer.tryNormalizeAabbAxis(aabb, axis, Direction.AxisDirection.NEGATIVE);
		double max1 = MorphNormalizer.tryNormalizeAabbAxis(aabb, axis, Direction.AxisDirection.POSITIVE);
		double min2 = MorphNormalizer.tryNormalizeAabbAxis(aabb2, axis, Direction.AxisDirection.NEGATIVE);
		double max2 = MorphNormalizer.tryNormalizeAabbAxis(aabb2, axis, Direction.AxisDirection.POSITIVE);
		return Math.max(Math.max(min1 - max2, min2 - max1), 0.0);
	}
}
