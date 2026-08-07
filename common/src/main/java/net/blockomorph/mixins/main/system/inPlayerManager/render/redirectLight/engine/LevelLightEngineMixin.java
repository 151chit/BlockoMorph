package net.blockomorph.mixins.main.system.inPlayerManager.render.redirectLight.engine;

import net.blockomorph.utils.side.Side;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.mixin.PrimitiveCancelSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelLightEngine.class, priority = 999) //for override forPoints
public class LevelLightEngineMixin {
	@Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

	@FastInject(method = "getRawBrightness", at = @At("HEAD"), continueIf = @PrimitiveCancelSignal(intValue = -1))
	private int check(BlockPos pos, int skyDampen) {
		var provider = LevelWithFlags.of(this.levelHeightAccessor).flags().customLightProvider;
		if (provider != null && Side.get() == provider.side()) {
			return provider.getMaxBrightness(pos);
		}
		return -1;
	}
}
