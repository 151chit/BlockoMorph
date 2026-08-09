package net.blockomorph.core.coords;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class InPlayerBlockPos {
	public static final int X_CHUNK_START = 60_000_000;
	public static final int X_CHUNK_END = X_CHUNK_START + 60_000_000;
	public static final int Y_CHUNK_START = 64;
	public static final int Y_CHUNK_END = Y_CHUNK_START + 30;
	public static final int MAX_BYTES_CODE_SIZE = 32 * 32 * 32;
	public static final int X_CENTER = Mth.lerpInt(0.5f, X_CHUNK_START, X_CHUNK_END);
	public static final InPlayerBlockPos ZERO = InPlayerBlockPos.get(0, 0, 0);
	public static final InPlayerBlockPos ONE = InPlayerBlockPos.get(1, 1, 1);
	public static final int ZERO_INT = ZERO.asInt();
	public final int x;
	public final int y;
	public final int z;

	public static boolean isMorphedPlayerBlockX(double x) {
		return x >= (double) X_CHUNK_START && x <= (double) X_CHUNK_END;
	}

	public static boolean isMorphedPlayerChunkX(int chunkX) {
		return isMorphedPlayerBlockX(SectionPos.sectionToBlockCoord(chunkX));
	}

	private InPlayerBlockPos(int x, int y, int z) {
		this.x = x; this.y = y; this.z = z;
	}

	public static InPlayerBlockPos get(int x, int y, int z) {
		return new InPlayerBlockPos(x, y, z);
	}

	public static InPlayerBlockPos decode(int pos) {
		if (isBytesInvalid(pos)) return null;
		int x = getX(pos);
		int y = getY(pos);
		int z = getZ(pos);
		if (isByteBoundInvalid(x, y, z)) return null;
		return InPlayerBlockPos.get(x, y, z);
	}

	public static int getX(int packed) { return (packed & 0x1F) - 16; }
	public static int getY(int packed) { return ((packed >> 5) & 0x1F) - 1; }
	public static int getZ(int packed) { return ((packed >> 10) & 0x1F) - 16; }

	public static int asInt(int x, int y, int z) {
		if (isByteBoundInvalid(x, y, z)) return -1;
		int nx = x + 16;
		int ny = y + 1;
		int nz = z + 16;
		return nx | (ny << 5) | (nz << 10);
	}

	public int asInt() { return asInt(this.x, this.y, this.z); }

	public int getX() { return x; }
	public int getY() { return y; }
	public int getZ() { return z; }

	public static boolean isValid(int pos) {
		if (isBytesInvalid(pos)) return false;
		return isValid(getX(pos), getY(pos), getZ(pos));
	}

	public static boolean isBytesInvalid(int pos) {
		return pos < 0 || pos >= MAX_BYTES_CODE_SIZE;
	}

	public static boolean isByteBoundInvalid(int x, int y, int z) {
		return (x > 15 || x < -16) || (z > 15 || z < -16) || (y > 30 || y < -1);
	}

	public static boolean isValid(int x, int y, int z) {
		return (x < 15 && x > -15) && (z < 15 && z > -15) && (y < 31 && y >= 0);
	}

	public static Integer isValidForCommand(Integer input, Direction.Axis axis) throws CommandSyntaxException {
		if (input == null) return null;
		if (axis == Direction.Axis.X || axis == Direction.Axis.Z) {
			if (input > 14) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(input, 14);
			if (input < -14) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(input, -14);
		} else {
			if (input < 0) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(input, 0);
			if (input > 30) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(input, 30);
		}
		return input;
	}

	public boolean isValid() {
		return isValid(this.x, this.y, this.z);
	}

	public int get(Direction.Axis axis) {
		return axis.choose(this.x, this.y, this.z);
	}

	public static boolean isInvalidPosFor(InPlayerManager mn, BlockPos pos) {
		int y = pos.getY() - mn.getZeroKey().getY();
		return Mth.abs(pos.getX() - mn.getZeroKey().getX()) >= 15 ||
				Mth.abs(pos.getZ() - mn.getZeroKey().getZ()) >= 15 ||
				y < 0 || y > 30;
	}

	public static int fromDelta(BlockPos zero, BlockPos morphed) {
		return asInt(
				morphed.getX() - zero.getX(),
				morphed.getY() - zero.getY(),
				morphed.getZ() - zero.getZ()
		);
	}

	@Nullable
	public BlockPos boundedBlockPos(Player player) {
		if (player != null) {
			long section = BlockPosBounds.getSectionByPlayer(player);
			if (section != -1) {
				int y = Y_CHUNK_START + this.y;
				int x = X_CHUNK_START + MorphedPlayerSection.getX(section) * MorphedPlayerSection.FOR_TWO_CHUNKS + this.x + MorphedPlayerSection.MAX_PLAYER_SIZE / 2;
				int z = MorphedPlayerSection.getZ(section) * MorphedPlayerSection.FOR_TWO_CHUNKS + MorphedPlayerSection.MAX_PLAYER_SIZE / 2 + this.z;
				return new BlockPos(x, y, z);
			}
		}
		return null;
	}

	public BlockPos encodeByManager(InPlayerManager mn) {
		BlockPos zero = mn.getZeroKey();
		return new BlockPos(zero.getX() + this.x, zero.getY() + this.y, zero.getZ() + this.z);
	}

	public static PlayerAccessor findPlayer(int blockPosX, int blockPosZ) {
		if (!isMorphedPlayerBlockX(blockPosX)) return null;
		long section = MorphedPlayerSection.fromMorphedBlockPos(blockPosX, blockPosZ);
		if (section == -1) return null;
		Player player = BlockPosBounds.getPlayerBySection(section);
		return PlayerAccessor.of(player);
	}

	public static PlayerAccessor findPlayer(BlockPos bounded) {
		return findPlayer(bounded.getX(), bounded.getZ());
	}

	public static int findInPlayerBlockPos(int blockPosX, int blockPosY, int blockPosZ) {
		if (!isMorphedPlayerBlockX(blockPosX)) return -1;
		long section = MorphedPlayerSection.fromMorphedBlockPos(blockPosX, blockPosZ);
		if (section == -1) return -1;
		return posBySection(blockPosX, blockPosY, blockPosZ, section);
	}

	public static int findInPlayerBlockPos(BlockPos bounded) {
		return findInPlayerBlockPos(bounded.getX(), bounded.getY(), bounded.getZ());
	}

	private static int posBySection(int blockPosX, int blockPosY, int blockPosZ, long section) {
		int y = blockPosY - Y_CHUNK_START;
		int x = blockPosX - MorphedPlayerSection.getX(section) * MorphedPlayerSection.FOR_TWO_CHUNKS - MorphedPlayerSection.MAX_PLAYER_SIZE / 2 - X_CHUNK_START;
		int z = blockPosZ - MorphedPlayerSection.getZ(section) * MorphedPlayerSection.FOR_TWO_CHUNKS - MorphedPlayerSection.MAX_PLAYER_SIZE / 2;
		return InPlayerBlockPos.asInt(x, y, z);
	}

	@Override
	public String toString() {
		return "InPlayerBlockPos[x=" + this.x + " y=" + this.y + " z=" + this.z + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof InPlayerBlockPos bl)
			return bl.x == this.x && bl.y == this.y && bl.z == this.z;
		return false;
	}

	@Override
	public int hashCode() {
		return (this.x + this.y * 29) * 29 + z;
	}
}
