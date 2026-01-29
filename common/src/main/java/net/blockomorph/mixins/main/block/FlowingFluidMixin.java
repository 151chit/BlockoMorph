package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

	@Inject(method = "spread", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadToSides(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;)V"), cancellable = true)
	public void checkMorphedInLiquidBlock(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			if (realPos.equals(InPlayerBlockPos.ZERO)) ci.cancel();
		}, null, serverLevel);
	}
}
