package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityCollisionContext.class)
public class EntityCollisionCtxMixin {
	@Shadow @Final @Nullable private Entity entity;
	@Shadow @Final private double entityBottom;

	@Inject(method = "isAbove", at = @At("HEAD"), cancellable = true)
	public void isAbove(VoxelShape voxelShape, BlockPos blockPos, boolean bl, CallbackInfoReturnable<Boolean> cir) {
		if (this.entity != null) {
			InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
				cir.setReturnValue(this.entityBottom > MorphUtils.getRealBlockPos(pl, realPos).y + voxelShape.max(Direction.Axis.Y) - (double) 1.0E-5F);
			}, () -> cir.setReturnValue(false), this.entity.level());
		}
	}

}
