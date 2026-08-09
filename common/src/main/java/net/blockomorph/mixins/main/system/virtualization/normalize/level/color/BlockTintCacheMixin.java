package net.blockomorph.mixins.main.system.virtualization.normalize.level.color;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockTintCache.class)
public class BlockTintCacheMixin {
	@Unique private final ThreadLocalMutableBlockPos blockPos = new ThreadLocalMutableBlockPos();

	@ModifyVariable(method = "getColor", at = @At("HEAD"))
	public BlockPos norm(BlockPos pos) {
		return MorphNormalizer.normalize(pos, this.blockPos);
	}
}
