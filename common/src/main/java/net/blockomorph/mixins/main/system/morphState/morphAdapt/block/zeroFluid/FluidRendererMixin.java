package net.blockomorph.mixins.main.system.morphState.morphAdapt.block.zeroFluid;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.mixin.PrimitiveCancelSignal;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlockRenderer.class)
public class FluidRendererMixin {
	@Unique private static final int[] CORNERS = new int[]{
			InPlayerBlockPos.get(0, 0, 1).asInt(), InPlayerBlockPos.get(0, 0, -1).asInt(),
			InPlayerBlockPos.get(1, 0, 0).asInt(), InPlayerBlockPos.get(-1, 0, 0).asInt()};

	@FastInject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"),
			method = "getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
			continueIf = @PrimitiveCancelSignal(floatValue = Float.NaN))
	public float getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos, BlockState state, FluidState fluidState) {
		if (level instanceof InPlayerBlockAndTintGetter playerProxy) {
			if (playerProxy.renderSize() == 1 && !playerProxy.getFluidState(playerProxy.getZeroKeyPos()).isEmpty()) {
				int heightPos = playerProxy.translateToOffset(pos);
				for (int corner : CORNERS) {
					if (corner == heightPos) return -1;
				}
			}
		}
		return Float.NaN;
	}
}
