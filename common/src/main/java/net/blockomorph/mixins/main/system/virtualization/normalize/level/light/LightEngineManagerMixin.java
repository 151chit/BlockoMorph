package net.blockomorph.mixins.main.system.virtualization.normalize.level.light;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelLightEngine.class)
public class LightEngineManagerMixin {
	@Unique private static final ThreadLocalMutableBlockPos BLOCKPOS = new ThreadLocalMutableBlockPos();

	@ModifyVariable(method = "getRawBrightness", at = @At("HEAD"))
	public BlockPos getRealRaw(BlockPos orig) {
		return MorphNormalizer.normalize(orig, BLOCKPOS);
	}
}
