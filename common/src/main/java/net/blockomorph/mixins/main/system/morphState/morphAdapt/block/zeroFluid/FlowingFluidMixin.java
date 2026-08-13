package net.blockomorph.mixins.main.system.morphState.morphAdapt.block.zeroFluid;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

	@FastInject(method = "spread", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadToSides(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	public boolean checkMorphedInLiquidBlock(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
		int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
		return posIn != InPlayerBlockPos.ZERO_INT;
	}
}
