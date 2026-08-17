package net.blockomorph.mixins.main.blockFix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronMixin {
	@Shadow protected abstract double getContentHeight(BlockState blockState);

	@ModifyReturnValue(method = "isEntityInsideContent", at = @At("RETURN"))
	private boolean test(boolean original, BlockState blockState, BlockPos blockPos, Entity entity) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(blockPos.getX())) {
			double realY = MorphNormalizer.normalizeOneOf(blockPos, Direction.Axis.Y);
			return entity.getY() < realY + this.getContentHeight(blockState) && entity.getBoundingBox().maxY > realY + (double) 0.25F;
		}
		return original;
	}
}
