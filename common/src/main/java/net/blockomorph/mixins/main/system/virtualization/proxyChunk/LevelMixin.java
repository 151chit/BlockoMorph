package net.blockomorph.mixins.main.system.virtualization.proxyChunk;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.proxyChunk.PlayerConnectingSource;
import net.blockomorph.core.coords.proxyChunk.ProxyChunksStorage;
import net.blockomorph.core.coords.proxyChunk.ProxyPlayerChunkHandler;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public class LevelMixin implements ProxyChunksStorage.Access {
	@Unique private final ProxyChunksStorage CHUNKS = new ProxyChunksStorage(this.getThis());

	@Override public ProxyChunksStorage getStorage() {
		return CHUNKS;
	}

	@FastInject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"))
	private Object overrideRealChunk(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(chunkX)) {
			ProxyChunksStorage storage = CHUNKS;
			if (this.getThis().getChunkSource() instanceof ProxyChunksStorage.Access st)
				storage = st.getStorage();
			var chunk = storage.getOrMakeChunk(chunkX, chunkZ);
			if (chunk != null) return chunk;
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	@FastInject(method = "getBlockState", at = @At("HEAD")) @SuppressWarnings("Invalid_FI_return_type")
	private Object fixLithiumBlockGetting(BlockPos pos) {
		return ProxyPlayerChunkHandler.getBlockState(PlayerConnectingSource.AUTOMATIC, this.getThis(), pos);
	}

	@FastInject(method = "isLoaded", at = @At("HEAD"))
	private byte fallbackIfChunkSourceUnaviable(BlockPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(pos.getX())) {
			if (!(this.getThis().getChunkSource() instanceof ProxyChunksStorage.Access)) {
				return (byte) (CHUNKS.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) ? 1 : -1);
			}
		}
		return 0;
	}

	@Unique
	private Level getThis() {
		return (Level) (Object)this;
	}
}
