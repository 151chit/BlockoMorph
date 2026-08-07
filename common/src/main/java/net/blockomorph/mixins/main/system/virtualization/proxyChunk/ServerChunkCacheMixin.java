package net.blockomorph.mixins.main.system.virtualization.proxyChunk;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.proxyChunk.ProxyChunksStorage;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin implements ProxyChunksStorage.Access {

	@Unique private ProxyChunksStorage chunksStorage;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(CallbackInfo ci, @Local(argsOnly = true) ServerLevel level) {
		this.chunksStorage = new ProxyChunksStorage(level);
	}

	@FastInject(require = 1, method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "HEAD"))
	public Object getChunk(int x, int z, ChunkStatus targetStatus, boolean loadOrGenerate) {
		var chunk = this.chunksStorage.getOrMakeChunk(x, z);
		if (chunk == null) return FastInject.CONTINUE_EXECUTION;
		return chunk;
	}

	@FastInject(require = 1, method = "getChunkNow", at = @At(value = "HEAD"))
	public Object getChunkImmediate(int x, int z) {
		var chunk = this.chunksStorage.getOrMakeChunk(x, z);
		if (chunk == null) return FastInject.CONTINUE_EXECUTION;
		return chunk;
	}

	@FastInject(require = 1, method = "hasChunk", at = @At("HEAD"))
	public byte hasChunk(int x, int z) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(x)) {
			return (byte) (this.chunksStorage.hasChunk(x, z) ? 1 : -1);
		}
		return 0;
	}

	@Override
	public ProxyChunksStorage getStorage() {
		return this.chunksStorage;
	}
}
