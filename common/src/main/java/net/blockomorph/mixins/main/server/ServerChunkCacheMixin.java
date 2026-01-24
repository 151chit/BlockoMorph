package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.FakeChunkStorage;
import net.blockomorph.utils.coords.DummyChunkStorage;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerChunkCache.class, priority = 20_000)
public class ServerChunkCacheMixin implements FakeChunkStorage {
	@Shadow @Final ServerLevel level;
	@Unique
	private final DummyChunkStorage CHUNKS = new DummyChunkStorage();

	@Inject(method = "blockChanged", at = @At(value = "HEAD"), cancellable = true)
	public void redirectChange(BlockPos pos, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			ci.cancel();
			pl.prepareSync(realPos);
		}, ci::cancel, this.level);
	}

	@Inject(require = 1, method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "HEAD"), cancellable = true)
	public void dummyChunk(int x, int z, ChunkStatus p_46504_, boolean p_46505_, CallbackInfoReturnable<ChunkAccess> cir) {
		CHUNKS.getFakeChunk(x, z, cir, this.level);
	}

	@Inject(require = 1, method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
	public void dummyChunkNow(int x, int z, CallbackInfoReturnable<ChunkAccess> cir) {
		CHUNKS.getFakeChunk(x, z, cir, this.level);
	}

	@Inject(require = 1, method = "hasChunk", at = @At("HEAD"), cancellable = true)
	public void hasDummyChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		CHUNKS.hasChunk(x, z, cir);
	}

	@ModifyVariable(require = 1, method = {
			"updateChunkForced",
			"getChunkDebugData"
	}, at = @At("HEAD"))
	public ChunkPos changePos(ChunkPos orig) {
		return MorphUtils.getChangedChunk(orig, this.level.isClientSide);
	}

	@Override
	public DummyChunkStorage getStorage() {
		return CHUNKS;
	}
}
