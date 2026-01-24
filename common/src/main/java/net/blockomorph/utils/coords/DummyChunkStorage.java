package net.blockomorph.utils.coords;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

public class DummyChunkStorage {
	private final HashMap<ChunkPos, DummyLevelChunk> FAKE_CHUNKS = new HashMap<>();

	public void hasChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphPlayerChunk(new ChunkPos(x, z))) {
			cir.setReturnValue(true);
		}
	}

	public void getFakeChunk(int x, int z, CallbackInfoReturnable<ChunkAccess> cir, Level lv) {
		ChunkPos pos = new ChunkPos(x, z);
		if (InPlayerBlockPos.isMorphPlayerChunk(pos)) {
			cir.setReturnValue(this.getFakeChunk(pos, lv));
		}
	}

	private DummyLevelChunk getFakeChunk(ChunkPos pos, Level level) {
		DummyLevelChunk chunk = FAKE_CHUNKS.get(pos);
		if (chunk != null && chunk.getLevel() == level) {
			return chunk;
		}
		chunk = new DummyLevelChunk(level, pos);
		FAKE_CHUNKS.put(pos, chunk);
		return chunk;
	}
}
