package net.blockomorph.mixins.main.system.virtualization.normalize.level.chunks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkPos.class)
public class ChunkPosMixin {

	@ModifyReturnValue(method = "isValid(II)Z", at = @At("RETURN"))
	private static boolean valid(boolean original, int x) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(x)) {
			return true;
		}
		return original;
	}
}
