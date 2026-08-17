package net.blockomorph.core.coords.proxyChunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ProxyChunksStorage {
	private final Level level;
	private final Long2ObjectMap<ProxyPlayerChunk> proxyChunks = new Long2ObjectOpenHashMap<>();

	public ProxyChunksStorage(Level level) {
		this.level = Objects.requireNonNull(level);
	}

	public void releaseAllChunksFor(long section) {
		if (section == -1) return;
		int blockX = InPlayerBlockPos.X_CHUNK_START + MorphedPlayerSection.getX(section) * MorphedPlayerSection.FOR_TWO_CHUNKS;
		int blockZ = MorphedPlayerSection.getZ(section) * MorphedPlayerSection.FOR_TWO_CHUNKS;
		int chunkX = SectionPos.blockToSectionCoord(blockX);
		int chunkZ = SectionPos.blockToSectionCoord(blockZ);
		this.removeChunk(chunkX, chunkZ);
		this.removeChunk(chunkX + 1, chunkZ);
		this.removeChunk(chunkX, chunkZ + 1);
		this.removeChunk(chunkX + 1, chunkZ + 1);
	}

	private void removeChunk(int chunkX, int chunkZ) {
		this.proxyChunks.remove(ChunkPos.asLong(chunkX, chunkZ));
	}

	public static void releasePlayerChunks(Player player) {
		long section = BlockPosBounds.getSectionByPlayer(player);
		if (section == -1) return;
		for (Level level : levelsFor(player)) {
			if (level instanceof Access st) {
				st.getStorage().releaseAllChunksFor(section);
			}
			if (level.getChunkSource() instanceof Access st) {
				st.getStorage().releaseAllChunksFor(section);
			}
		}
	}

	public boolean hasChunk(int chunkX, int chunkZ) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(chunkX)) {
			return this.getOrMakeChunk(chunkX, chunkZ) != null;
		}
		return false;
	}

	@Nullable
	public LevelChunk getOrMakeChunk(int chunkX, int chunkZ) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(chunkX) && Side.get() != Side.UNKNOWN) {
			long chunkPos = ChunkPos.asLong(chunkX, chunkZ);
			ProxyPlayerChunk chunk = this.proxyChunks.get(chunkPos);
			if (chunk != null) return chunk;
			if (BlockPosBounds.getPlayerBySection(MorphedPlayerSection.fromMorphedChunk(chunkX, chunkZ)) == null) {
				return this.empty(chunkPos);
			}
			chunk = new ProxyPlayerChunk(this.level, new ChunkPos(chunkX, chunkZ));
			this.proxyChunks.put(chunkPos, chunk);
			return chunk;
		}
		return null;
	}

	private LevelChunk empty(long chunkPos) {
		return new EmptyLevelChunk(this.level, new ChunkPos(chunkPos), this.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
	}

	@FunctionalInterface
	public interface Access {
		ProxyChunksStorage getStorage();
	}

	private static Iterable<? extends Level> levelsFor(Player player) {
		if (Side.get() == Side.CLIENT) {
			Level lv = MorphUtils.Client.getLevel();
			if (lv != null) return List.of(lv);
			return List.of();
		}
		if (player instanceof ServerPlayer pl) {
			return pl.level().getServer().getAllLevels();
		}
		return List.of(player.level());
	}
}
