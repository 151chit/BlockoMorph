package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.coords.DummyChunkStorage;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
    @Shadow @Final ClientLevel level;
    @Unique
    private final DummyChunkStorage CHUNKS = new DummyChunkStorage();

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At(value = "HEAD"), cancellable = true)
    public void dummyChunk(int x, int z, ChunkStatus p_46504_, boolean p_46505_, CallbackInfoReturnable<ChunkAccess> cir) {
        CHUNKS.getFakeChunk(x, z, cir, this.level);
    }
}
