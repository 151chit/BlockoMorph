package net.blockomorph.core.coords;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public record MorphedPlayerSection(int x, int z) {
	public MorphedPlayerSection(long section) {
		this(getX(section), getZ(section));
	}

	private static final RandomSource RANDOM = RandomSource.create();
	private static final long COORD_MASK = 4294967295L;
	public static final int MAX_PLAYER_SECTION = 1_875_000;
	public static final int MAX_PLAYER_SIZE = 31;
	public static final int FOR_TWO_CHUNKS = 32;

	public static long pack(int x, int z) {
		if (x < 0 || z < 0) throw new IllegalArgumentException("Negative section!");
		return (long) x & COORD_MASK | ((long) z & COORD_MASK) << 32;
	}

	public static boolean isInvalid(long section) {
		if (section < 0) return true;
		return isAxisInvalid(section, Direction.Axis.X) || isAxisInvalid(section, Direction.Axis.Z);
	}

	public static boolean isAxisInvalid(long section, Direction.Axis axis) {
		int cord = switch (axis) {
			case X -> getX(section);
			case Z -> getZ(section);
			default -> throw new IllegalArgumentException(axis.getName());
		};
		return cord < 0 || cord >= MAX_PLAYER_SECTION;
	}

	public static int getX(final long pos) { return (int)(pos & COORD_MASK); }
	public static int getZ(final long pos) { return (int)(pos >>> 32 & COORD_MASK); }

	public static long createNew() {// [0, MAX_PLAYER_SECTION)
		int x = RANDOM.nextInt(MAX_PLAYER_SECTION);
		int z = RANDOM.nextInt(MAX_PLAYER_SECTION);
		return pack(x, z);
	}

	public static long fromMorphedBlockPos(int morphedBlockX, int morphedBlockZ) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(morphedBlockX)) return -1;
		int sectorX = (morphedBlockX - InPlayerBlockPos.X_CHUNK_START) / MorphedPlayerSection.FOR_TWO_CHUNKS;
		int sectorZ = morphedBlockZ / MorphedPlayerSection.FOR_TWO_CHUNKS;
		return pack(sectorX, sectorZ);
	}

	public static long fromMorphedChunk(int chunkX, int chunkZ) {
		if (!InPlayerBlockPos.isMorphedPlayerChunkX(chunkX)) return -1;
		return fromMorphedBlockPos(SectionPos.sectionToBlockCoord(chunkX), SectionPos.sectionToBlockCoord(chunkZ));
	}

	public static MorphedPlayerSection forLog(InPlayerManager player) {
		long section = BlockPosBounds.getSectionByPlayer(player.getOwner().player());
		if (section == -1) return null;
		return new MorphedPlayerSection(section);
	}

	public static Player checkOnBoundPlayer(ChunkPos bounded) {
		if (!InPlayerBlockPos.isMorphedPlayerChunkX(bounded.x())) return null;
		long section = fromMorphedChunk(bounded.x(), bounded.z());
		return BlockPosBounds.getPlayerBySection(section);
	}

	@Override
	public String toString() {
		return "PlayerMorphedSection[" + this.x + ", " + this.z + "]";
	}
}
