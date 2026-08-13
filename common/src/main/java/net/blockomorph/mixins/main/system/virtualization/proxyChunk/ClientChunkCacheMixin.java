package net.blockomorph.mixins.main.system.virtualization.proxyChunk;

import net.blockomorph.core.coords.proxyChunk.ProxyChunksStorage;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientChunkCache.class)
public class ClientChunkCacheMixin implements ProxyChunksStorage.Access {
	@Unique private ProxyChunksStorage chunksStorage;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(ClientLevel level, int serverChunkRadius, CallbackInfo ci) {
		this.chunksStorage = new ProxyChunksStorage(level);
	}

	@FastInject(require = 1, method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At(value = "HEAD"))
	public Object getChunk(int x, int z, ChunkStatus targetStatus, boolean loadOrGenerate) {
		var chunk = this.chunksStorage.getOrMakeChunk(x, z);
		if (chunk == null) return FastInject.CONTINUE_EXECUTION;
		return chunk;
	}

	@Override
	public ProxyChunksStorage getStorage() {
		return this.chunksStorage;
	}
}
