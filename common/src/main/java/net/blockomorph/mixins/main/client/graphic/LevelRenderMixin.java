package net.blockomorph.mixins.main.client.graphic;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.*;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin implements LevelRendererAccessor {
	@Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks() {
		return this.destructionProgress;
	}

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	public void rejectChunkUpdateOnPlayer(int chunkX, int sectionY, int chunkZ, boolean yes, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphPlayerChunk(new ChunkPos(chunkX, chunkZ)))
			ci.cancel();
	}
}
