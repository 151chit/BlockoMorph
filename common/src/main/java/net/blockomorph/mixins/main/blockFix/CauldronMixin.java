package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronMixin {

	@Shadow
	protected abstract double getContentHeight(BlockState blockState);

	@Inject(method = "isEntityInsideContent", at = @At("HEAD"), cancellable = true)
	private void test(BlockState blockState, BlockPos blockPos, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(blockPos.getX())) {
			double realY = InPlayerBlockPos.checkOnReal(Vec3.atLowerCornerOf(blockPos)).y();
			boolean result = entity.getY() < realY + this.getContentHeight(blockState) && entity.getBoundingBox().maxY > realY + (double) 0.25F;
			cir.setReturnValue(result);
		}
	}
}
