package net.blockomorph.mixins.main.block.morphAdapt;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

	@Inject(method = "spread", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadToSides(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;)V"), cancellable = true)
	public void checkMorphedInLiquidBlock(Level level, BlockPos blockPos, FluidState fluidState, CallbackInfo ci) {
		InPlayerBlockPos pos = InPlayerBlockPos.getBlockPosInPlayer(blockPos);
		if (pos != null && pos.equals(InPlayerBlockPos.ZERO)) ci.cancel();
	}
}
