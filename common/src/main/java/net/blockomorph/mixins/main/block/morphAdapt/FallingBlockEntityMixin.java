package net.blockomorph.mixins.main.block.morphAdapt;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

	@Inject(method = "fall", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), cancellable = true)
	private static void fall(Level lv, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, @Local FallingBlockEntity fallingblockentity) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (realPos.equals(InPlayerBlockPos.ZERO)) {
				cir.setReturnValue(fallingblockentity);
			}
		}, () -> cir.setReturnValue(fallingblockentity), lv);
	}
}
