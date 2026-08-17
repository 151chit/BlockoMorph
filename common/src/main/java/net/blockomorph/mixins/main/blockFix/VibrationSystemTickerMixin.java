package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VibrationSystem.Ticker.class)
public interface VibrationSystemTickerMixin {

	@FastInject(method = "areAdjacentChunksTicking", at = @At("HEAD"))
	private static byte suppress(Level level, BlockPos listenerPos) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(listenerPos.getX())) return 1;
		return 0;
	}
}
