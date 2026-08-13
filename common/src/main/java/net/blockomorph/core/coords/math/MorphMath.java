package net.blockomorph.core.coords.math;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.HitBoxCalculator;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MorphMath {

	public static Vec3 getRealBlockPos(PlayerAccessor original, Vec3 offset) {
		return getRealBlockPos(original.player().position(), original.minPos(), original.maxPos(), offset.x, offset.y, offset.z);
	}

	public static Vec3 getRealBlockPos(PlayerAccessor original, InPlayerBlockPos offset) {
		return getRealBlockPos(original.player().position(), original.minPos(), original.maxPos(), offset.x, offset.y, offset.z);
	}

	public static Vec3 getRealBlockPos(Vec3 playerPos, InPlayerBlockPos minPos, InPlayerBlockPos maxPos, double x, double y, double z) {
		double realX = getRealBlockPosAxis(Direction.Axis.X, playerPos, minPos, maxPos, x);
		double realY = getRealBlockPosAxis(Direction.Axis.Y, playerPos, minPos, maxPos, y);
		double realZ = getRealBlockPosAxis(Direction.Axis.Z, playerPos, minPos, maxPos, z);
		return new Vec3(realX, realY, realZ);
	}

	public static double getRealBlockPosAxis(Direction.Axis axis, Vec3 playerPos, InPlayerBlockPos minPos, InPlayerBlockPos maxPos, double inPlayerCoord) {
		return getRealBlockPosAxis(axis, playerPos.get(axis), minPos, maxPos, inPlayerCoord);
	}

	public static double getRealBlockPosAxis(Direction.Axis axis, double coord, InPlayerBlockPos minPos, InPlayerBlockPos maxPos, double inPlayerCoord) {
		if (axis == Direction.Axis.Y) {
			return coord + (inPlayerCoord - minPos.getY());
		}
		double width = maxPos.get(axis) - minPos.get(axis);
		double corner = coord - (width / 2);
		double delta = inPlayerCoord - minPos.get(axis);
		return corner + delta;
	}

	public static int offsetSectionIndex(SectionPos pos) {
		return offsetSectionIndex(pos.x(), pos.y(), pos.z());
	}

	public static int offsetSectionIndex(int x, int y, int z) {
		if (x > 0 || x < -1 || z > 0 || z < -1 || y < 0 || y > 1) return -1;
		int nx = x + 1;
		int nz = z + 1;
		return nx | (nz << 1) | (y << 2);
	}

	@Nullable
	public static BlockInPlayer2 getBlockInPlayerOnPos(PlayerAccessor pl, double x, double y, double z) {
		double blockX = x - getRealBlockPosCenter(Direction.Axis.X, pl);
		double blockY = y - getRealBlockPosCenter(Direction.Axis.Y, pl);
		double blockZ = z - getRealBlockPosCenter(Direction.Axis.Z, pl);
		int pos = InPlayerBlockPos.asInt(Mth.floor(blockX), Mth.floor(blockY), Mth.floor(blockZ));
		return pl.getBlock(pos);
	}

	public static double getRealBlockPosAxis(Direction.Axis axis, PlayerAccessor pl, double inPlayerCoord) {
		return getRealBlockPosAxis(axis, pl.player().position(), pl.minPos(), pl.maxPos(), inPlayerCoord);
	}

	public static double getRealBlockPosAxis(Direction.Axis axis, BlockInPlayer2 block) {
		PlayerAccessor pl = block.getPlayer();
		return getRealBlockPosAxis(axis, pl.player().position(), pl.minPos(), pl.maxPos(), axis.choose(block.getOffset().x, block.getOffset().y, block.getOffset().z));
	}

	public static double getRealBlockPosCenter(Direction.Axis axis, PlayerAccessor pl) {
		return getRealBlockPosAxis(axis, pl, 0);
	}

	public static double adjustMatrixForPlayer(PlayerAccessor pl, Direction.Axis axis) {
		if (axis == Direction.Axis.Y) throw new IllegalArgumentException();
		InPlayerBlockPos minpos = pl.minPos();
		InPlayerBlockPos maxpos = pl.maxPos();

		int width = maxpos.get(axis) - minpos.get(axis);
		return -((double) width / 2) - minpos.get(axis);
	}

	public static double lerpedPlayerPos(PlayerAccessor pl, Direction.Axis axis, float deltaTick) {
		return Mth.lerp(deltaTick, pl.player().oldPosition().get(axis), pl.player().position().get(axis));
	}

	public static Vec3 playerSectionOrigin(PlayerAccessor pl, float deltaTick) {
		Vec3 lerpedPos = pl.player().oldPosition().lerp(pl.player().position(), deltaTick);
		double originX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, lerpedPos, pl.minPos(), pl.maxPos(), 0);
		double originY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, lerpedPos, pl.minPos(), pl.maxPos(), 0);
		double originZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, lerpedPos, pl.minPos(), pl.maxPos(), 0);
		return new Vec3(originX, originY, originZ);
	}

	public static void playerPosForRender(PlayerAccessor pl, float deltaTick, Vec3 camPos, RenderVecOffset offset) {
		offset.offset(
				(float) (lerpedPlayerPos(pl, Direction.Axis.X, deltaTick) - camPos.x + adjustMatrixForPlayer(pl, Direction.Axis.X)),
				(float) (lerpedPlayerPos(pl, Direction.Axis.Y, deltaTick) - camPos.y),
				(float) (lerpedPlayerPos(pl, Direction.Axis.Z, deltaTick) - camPos.z + adjustMatrixForPlayer(pl, Direction.Axis.Z))
		);
	}

	@FunctionalInterface
	public interface RenderVecOffset {
		void offset(float x, float y, float z);
	}

	public static HitBoxCalculator.HitboxData ifHitboxDataChanged(HitBoxCalculator.HitboxData data, PlayerAccessor pl) {
		Vec3 newPos = pl.player().position();
		InPlayerBlockPos min = pl.minPos();
		InPlayerBlockPos max = pl.maxPos();
		if (data == null || !newPos.equals(data.position()) || !min.equals(data.minPos()) || !max.equals(data.maxPos())) {
			return new HitBoxCalculator.HitboxData(newPos, min, max);
		}
		return null;
	}

	public static HitBoxCalculator.HitboxData isBlockPosChanged(HitBoxCalculator.HitboxData data, PlayerAccessor pl) {
		Vec3 newPos = pl.player().position();
		InPlayerBlockPos min = pl.minPos();
		InPlayerBlockPos max = pl.maxPos();
		if (data == null || isBlockPosChanged(data.position(), newPos, data.minPos(), data.maxPos(), min, max)) {
			return new HitBoxCalculator.HitboxData(newPos, min, max);
		}
		return null;
	}

	public static boolean isBlockPosChanged(Vec3 old, Vec3 pos, InPlayerBlockPos oldMin, InPlayerBlockPos oldMax, InPlayerBlockPos minPos, InPlayerBlockPos maxPos) {
		double fullWidthOld = oldMax.getX() - oldMin.getX();
		double fullDepthOld = oldMax.getZ() - oldMin.getZ();

		double fullWidthNew = maxPos.getX() - minPos.getX();
		double fullDepthNew = maxPos.getZ() - minPos.getZ();

		double cornerXOld = old.x - (fullWidthOld / 2.0);
		double cornerZOld = old.z - (fullDepthOld / 2.0);

		double cornerXNew = pos.x - (fullWidthNew / 2.0);
		double cornerZNew = pos.z - (fullDepthNew / 2.0);

		return Mth.floor(cornerXOld + 0.5) != Mth.floor(cornerXNew + 0.5) ||
				Mth.floor(old.y) != Mth.floor(pos.y) ||
				Mth.floor(cornerZOld + 0.5) != Mth.floor(cornerZNew + 0.5);
	}
}