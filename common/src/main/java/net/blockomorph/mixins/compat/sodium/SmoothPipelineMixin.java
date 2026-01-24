package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline")
public class SmoothPipelineMixin {
	@Unique private static final long minPos = new BlockPos(InPlayerBlockPos.X_CHUNK_START, 0, 0).asLong();
	@Unique private static final long maxPos = new BlockPos(InPlayerBlockPos.X_CHUNK_END, 0, 0).asLong();

	@Shadow private long cachedPos;

	@Inject(method = "updateCachedData", at = @At("HEAD"))
	private void removeCacheForPlayerBlocks(long key, CallbackInfo ci) {
		int x = BlockPos.getX(key);
		if (x >= BlockPos.getX(minPos) && x <= BlockPos.getX(maxPos)) {
			this.cachedPos = 0;
		}
	}
}
