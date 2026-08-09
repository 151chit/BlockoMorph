package net.blockomorph.mixins.main.system.virtualization.normalize.level.light;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightEngine.class)
public class LightEngineMixin {
	@Unique private static final ThreadLocalMutableBlockPos BLOCKPOS = new ThreadLocalMutableBlockPos();

	@ModifyVariable(method = "getLightValue", at = @At("HEAD"))
	public BlockPos getReal(BlockPos orig) {
		return MorphNormalizer.normalize(orig, BLOCKPOS);
	}
}
