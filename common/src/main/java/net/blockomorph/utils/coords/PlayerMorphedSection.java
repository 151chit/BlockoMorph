package net.blockomorph.utils.coords;

import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.FakeChunkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayerMorphedSection {
	public static final int MAX_SIZE = 31;
	public static final int FOR_TWO_CHUNKS = 32;
	public final int x;
	public final int z;

	public PlayerMorphedSection(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public PlayerMorphedSection(long encoded) {
		this.x = (int) encoded;
		this.z = (int) (encoded >> 32);
	}

	@Nullable
	public static Player getPlayerByChunkPos(ChunkPos real, @Nullable Boolean client) {
		if (!InPlayerBlockPos.isMorphPlayerChunk(real)) return null;
		BlockPos bounded = new BlockPos(real.getBlockAt(7, 0, 7));
		PlayerMorphedSection section = BlockPosBounds.getPlayerSectionPos(bounded);
		return BlockPosBounds.getPlayerByChunkPos(section, client);
	}

	@Nullable
	public static ChunkPos[] getPlayerChunks(Player player) {
		PlayerMorphedSection morphedSection = BlockPosBounds.getChunkPosForPlayer(player);
		if (morphedSection == null) return null;
		int chunksCount = FOR_TWO_CHUNKS / 16;
		int blockX = InPlayerBlockPos.X_CHUNK_START + morphedSection.x * FOR_TWO_CHUNKS;
		int blockZ = morphedSection.z * FOR_TWO_CHUNKS;
		int i = 0;
		ChunkPos[] poses = new ChunkPos[chunksCount * chunksCount];
		for (int x = 1; x <= chunksCount; x++) {
			for (int z = 1; z <= chunksCount; z++) {
				poses[i] = new ChunkPos(SectionPos.blockToSectionCoord(blockX + x*8), SectionPos.blockToSectionCoord(blockZ + z*8));
				i++;
			}
		}
		return poses;
	}

	public static void cleanProxyChunks(Player old) {
		ChunkPos[] chunks = getPlayerChunks(old);
		if (old.level() instanceof FakeChunkStorage st) {
			DummyChunkStorage storage = st.getStorage();
			if (storage != null) storage.cleanFakeChunk(chunks);
			if (old.level().getChunkSource() instanceof FakeChunkStorage chunkSt) {
				DummyChunkStorage storageChunk = chunkSt.getStorage();
				if (storageChunk != null) storageChunk.cleanFakeChunk(chunks);
			}
		}
	}

	@Nullable
	public static Long2LongMap absoluteToOffset(PlayerAccessor pl) {
		PlayerMorphedSection morphedSection = BlockPosBounds.getChunkPosForPlayer(pl.player());
		if (morphedSection == null) return null;
		Long2LongMap map = new Long2LongArrayMap();
		int chunksCount = FOR_TWO_CHUNKS / 16;
		int blockX = InPlayerBlockPos.X_CHUNK_START + morphedSection.x * FOR_TWO_CHUNKS;
		int blockZ = morphedSection.z * FOR_TWO_CHUNKS;
		for (int x = 1; x <= chunksCount; x++) {
			for (int z = 1; z <= chunksCount; z++) {
				long absolute = ChunkPos.pack(SectionPos.blockToSectionCoord(blockX + x*8), SectionPos.blockToSectionCoord(blockZ + z*8));
				long offset = ChunkPos.pack(SectionPos.blockToSectionCoord(x*8), SectionPos.blockToSectionCoord(z*8));
				map.put(absolute, offset);
			}
		}
		return map;
	}

	public long toLong() {
		return ChunkPos.pack(this.x, this.z);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.x, this.z);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof PlayerMorphedSection section) {
			return section.x == this.x && section.z == this.z;
		}
		return false;
	}

	@Override
	public String toString() {
		return "PlayerMorphedSection[" + this.x + ", " + this.z + "]";
	}
}
