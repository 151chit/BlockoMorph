package net.blockomorph.utils;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MorphMath {

	public static Vec3 getRealBlockPos(PlayerAccessor original, InPlayerBlockPos offset) {
		return getRealBlockPos(original.player().position(), original.minPos(), original.maxPos(), offset.x, offset.y, offset.z);
	}

	public static Vec3 getRealBlockPos(PlayerAccessor original, double x, double y, double z) {
		return getRealBlockPos(original.player().position(), original.minPos(), original.maxPos(), x, y, z);
	}

	public static Vec3 getRealBlockPos(Vec3 playerPos, InPlayerBlockPos minPos, InPlayerBlockPos maxPos, double x, double y, double z) {
		double fullWidth = maxPos.getX() - minPos.getX();
		double fullDepth = maxPos.getZ() - minPos.getZ();
		double cornerX = playerPos.x - (fullWidth / 2);
		double cornerY = playerPos.y;
		double cornerZ = playerPos.z - (fullDepth / 2);

		double deltaX = x - minPos.getX();
		double deltaY = y - minPos.getY();
		double deltaZ = z - minPos.getZ();

		return new Vec3(cornerX + deltaX, cornerY + deltaY, cornerZ + deltaZ);
	}

	public static Vec3 getCenteredRealBlockPos(PlayerAccessor original, InPlayerBlockPos offset) {
		Vec3 vec = getRealBlockPos(original, offset);
		return new Vec3(vec.x + 0.5, vec.y + 0.5, vec.z + 0.5);
	}

	public static boolean isBlockPosChanged(Vec3 old, Vec3 newPos, InPlayerBlockPos oldMin, InPlayerBlockPos oldMax, InPlayerBlockPos minPos, InPlayerBlockPos maxPos) {
		return isBlockPosChanged(old, newPos.x, newPos.y, newPos.z, oldMin, oldMax, minPos, maxPos);
	}

	public static boolean isBlockPosChanged(Vec3 old, double newX, double newY, double newZ,
	                                        InPlayerBlockPos oldMin, InPlayerBlockPos oldMax, InPlayerBlockPos minPos, InPlayerBlockPos maxPos) {
		double fullWidthOld = oldMax.getX() - oldMin.getX();
		double fullDepthOld = oldMax.getZ() - oldMin.getZ();

		double fullWidthNew = maxPos.getX() - minPos.getX();
		double fullDepthNew = maxPos.getZ() - minPos.getZ();

		double cornerXOld = old.x - (fullWidthOld / 2.0);
		double cornerZOld = old.z - (fullDepthOld / 2.0);

		double cornerXNew = newX - (fullWidthNew / 2.0);
		double cornerZNew = newZ - (fullDepthNew / 2.0);

		return Mth.floor(cornerXOld + 0.5) != Mth.floor(cornerXNew + 0.5) ||
				Mth.floor(old.y) != Mth.floor(newY) ||
				Mth.floor(cornerZOld + 0.5) != Mth.floor(cornerZNew + 0.5);
	}

	public static double distanceTo(double orig,
	                                double fromX, double fromY, double fromZ,
	                                double toX, double toY, double toZ,
	                                boolean sqr, double offset) {
		boolean fromMorphed = InPlayerBlockPos.isMorphedPlayerX(fromX);
		boolean toMorphed = InPlayerBlockPos.isMorphedPlayerX(toX);
		if (fromMorphed || toMorphed) {
			if (fromMorphed) {
				Vec3 from = InPlayerBlockPos.checkOnReal(new Vec3(fromX, fromY, fromZ));
				fromX = from.x;
				fromY = from.y;
				fromZ = from.z;
			} if (toMorphed) {
				Vec3 to = InPlayerBlockPos.checkOnReal(new Vec3(toX, toY, toZ));
				toX = to.x;
				toY = to.y;
				toZ = to.z;
			}
			double d0 = fromX + offset - toX;
			double d1 = fromY + offset - toY;
			double d2 = fromZ + offset - toZ;
			double result = d0 * d0 + d1 * d1 + d2 * d2;
			if (!sqr)
				result = Math.sqrt(result);
			return result;
		}
		return orig;
	}

	public static double distanceTo(double orig, Vec3 from, double x, double y, double z, boolean sqr, double offset) {
		return distanceTo(orig, from.x, from.y, from.z, x ,y, z, sqr, offset);
	}

	public static double distanceTo(double orig, Vec3 from, Vec3 to, boolean sqr, double offset) {
		return distanceTo(orig, from.x, from.y, from.z, to.x, to.y, to.z, sqr, offset);
	}
}
