package net.blockomorph.utils.coords;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class DummyChunkStorage {
	private final Long2ObjectMap<DummyLevelChunk> FAKE_CHUNKS = new Long2ObjectOpenHashMap<>();

	public void cleanFakeChunk(ChunkPos[] chunks) {
		if (chunks == null) return;
		for (ChunkPos pos : chunks) {
			if (!InPlayerBlockPos.isMorphPlayerChunk(pos)) continue;
			FAKE_CHUNKS.remove(this.packCoords(pos.x(), pos.z()));
		}
	}

	public void hasChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(SectionPos.sectionToBlockCoord(x))) {
			cir.setReturnValue(true);
		}
	}

	public void getFakeChunk(int x, int z, CallbackInfoReturnable<ChunkAccess> cir, Level lv) {
		if (InPlayerBlockPos.isMorphedPlayerX(SectionPos.sectionToBlockCoord(x))) {
			long pos = this.packCoords(x, z);
			cir.setReturnValue(this.getFakeChunk(pos, lv));
		}
	}

	@Nullable
	public DummyLevelChunk getFakeChunk(int x, int z, Level lv) {
		if (InPlayerBlockPos.isMorphedPlayerX(SectionPos.sectionToBlockCoord(x))) {
			long pos = this.packCoords(x, z);
			return this.getFakeChunk(pos, lv);
		}
		return null;
	}

	private long packCoords(int x, int z) {
		int xPacked = x - InPlayerBlockPos.X_CENTER;
		int zPacked = z - InPlayerBlockPos.X_CHUNK_START / 2;
		return ChunkPos.pack(xPacked, zPacked);
	}

	private DummyLevelChunk getFakeChunk(long pos, Level level) {
		DummyLevelChunk chunk = FAKE_CHUNKS.get(pos);
		if (chunk != null && chunk.getLevel() == level) {
			return chunk;
		}
		int x = ChunkPos.getX(pos);
		int z = ChunkPos.getZ(pos);
		chunk = new DummyLevelChunk(level, new ChunkPos(x + InPlayerBlockPos.X_CENTER, z + InPlayerBlockPos.X_CHUNK_START / 2));
		FAKE_CHUNKS.put(pos, chunk);
		return chunk;
	}
}
