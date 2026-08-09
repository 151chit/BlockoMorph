package net.blockomorph.mixins.main.system.virtualization.normalize.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public class LevelBoundsMixin {

	@ModifyReturnValue(method = "isInWorldBoundsHorizontal", at = @At(value = "RETURN"))
	private static boolean acceptIfPlayerPos(boolean original, BlockPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(pos.getX())) return true;
		return original;
	}

	@ModifyExpressionValue(method = "getHeight", at = {
			@At(value = "CONSTANT", args = "intValue=30000000", ordinal = 0),
			@At(value = "CONSTANT", args = "intValue=30000000", ordinal = 1)
	})
	private int acceptIfPlayerPosMin(int original) {
		return Integer.MAX_VALUE;
	}

	@ModifyExpressionValue(method = "getHeight", at = {
			@At(value = "CONSTANT", args = "intValue=-30000000", ordinal = 0),
			@At(value = "CONSTANT", args = "intValue=-30000000", ordinal = 1)
	})
	private int acceptIfPlayerPosMax(int original) {
		return Integer.MIN_VALUE;
	}
}
