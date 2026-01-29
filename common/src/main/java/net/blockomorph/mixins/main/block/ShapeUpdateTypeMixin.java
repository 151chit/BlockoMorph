package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.redstone.CollectingNeighborUpdater$ShapeUpdate")
public class ShapeUpdateTypeMixin {
	@Shadow @Final private Direction direction;
	@Shadow @Final private BlockPos pos;
	@Shadow @Final private BlockPos neighborPos;
	@Shadow @Final private int updateFlags;
	@Shadow @Final private int updateLimit;
	@Shadow @Final private BlockState neighborState;

	@Inject(method = "runNext", at = @At("HEAD"), require = 1, cancellable = true)
	public void redirectUpdate(Level lv, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(this.pos, (pl, realPos) -> {
			cir.setReturnValue(false);
			MorphUtils.executeMorphedBlockShapeUpdate(lv, this.direction, this.pos, this.neighborPos, this.neighborState, this.updateFlags, this.updateLimit, pl.getBlockState(realPos));
		}, null, lv);
	}
}
