package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LiquidBlockRenderer.class)
public class LiquidRendererMixin {
	@Unique private static final List<InPlayerBlockPos> CORNERS = List.of(InPlayerBlockPos.get(0, 0, 1), InPlayerBlockPos.get(0, 0, -1), InPlayerBlockPos.get(1, 0, 0), InPlayerBlockPos.get(-1, 0, 0));

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"),
			method = "getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F", cancellable = true)
	public void getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos offsetted, BlockState blockState, FluidState fluidState, CallbackInfoReturnable<Float> cir) {
		InPlayerBlockPos.check(offsetted, (pl, realPos) -> {
			if (pl.getBlocksData2().size() == 1 && pl.getBlocksData2().get(InPlayerBlockPos.ZERO) != null) {
				if (CORNERS.contains(realPos)) {
					cir.setReturnValue(-1f);
				}
			}
		}, null, true);
	}
}
