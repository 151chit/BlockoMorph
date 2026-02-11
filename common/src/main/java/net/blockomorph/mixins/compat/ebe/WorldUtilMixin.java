package net.blockomorph.mixins.compat.ebe;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "foundationgames.enhancedblockentities.util.WorldUtil", remap = false)
public class WorldUtilMixin {

	@Inject(method = "rebuildChunkAndThen", at = @At("HEAD"), cancellable = true)
	private static void doNotHandleChunkAndRunImmediate(Level world, BlockPos pos, Runnable action, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
			ci.cancel();
			action.run();
		}
	}
}
