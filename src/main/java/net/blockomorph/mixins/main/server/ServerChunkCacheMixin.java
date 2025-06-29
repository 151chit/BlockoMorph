package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.coords.DummyChunkStorage;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
	@Shadow @Final
	private ServerLevel level;
	@Unique
	private final DummyChunkStorage CHUNKS = new DummyChunkStorage();

	@Inject(method = "blockChanged", at = @At(value = "HEAD"), cancellable = true)
	public void redirectChange(BlockPos pos, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			ci.cancel();
			pl.prepareSync(realPos);
		}, ci::cancel, this.level);
	}

	@Inject(method = "getChunk", at = @At(value = "HEAD"), cancellable = true)
	public void dummyChunk(int x, int z, ChunkStatus p_46504_, boolean p_46505_, CallbackInfoReturnable<ChunkAccess> cir) {
		CHUNKS.getFakeChunk(x, z, cir, this.level);
	}

	@Inject(method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
	public void dummyChunkNow(int x, int z, CallbackInfoReturnable<ChunkAccess> cir) {
		CHUNKS.getFakeChunk(x, z, cir, this.level);
	}

	@Inject(method = "hasChunk", at = @At("HEAD"), cancellable = true)
	public void hasDummyChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		CHUNKS.hasChunk(x, z, cir);
	}

	@ModifyVariable(method = {
			"updateChunkForced",
			"getChunkDebugData"
	}, at = @At("HEAD"))
	public ChunkPos changePos(ChunkPos orig) {
		return this.getChangedChunk(orig);
	}

	@ModifyVariable(method = {
			"removeRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V",
			"addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"
	}, at = @At("HEAD"))
	public ChunkPos changePosRemove(ChunkPos orig) {
		return this.getChangedChunk(orig);
	}

	@Unique
	private ChunkPos getChangedChunk(ChunkPos orig) {
		Player pl = PlayerMorphedSection.getPlayerByChunkPos(orig, this.level.isClientSide);
		if (pl != null) {
			return pl.chunkPosition();
		}
		if (InPlayerBlockPos.isMorphPlayerChunk(orig)) {
			return ChunkPos.ZERO;
		}
		return orig;
	}
}
