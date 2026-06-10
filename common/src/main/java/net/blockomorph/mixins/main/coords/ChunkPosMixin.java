package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkPos.class)
public class ChunkPosMixin {

	@Inject(require = 1, method = "isValid(II)Z", at = @At("HEAD"), cancellable = true)
	private static void valid(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphPlayerChunk(new ChunkPos(x, z))) {
			cir.setReturnValue(true);
		}
	}
}
