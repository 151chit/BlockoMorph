package net.blockomorph.mixins.main.client.graphic;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(FluidRenderer.class)
public class LiquidRendererMixin {
	@Unique private static final List<InPlayerBlockPos> CORNERS = List.of(InPlayerBlockPos.get(0, 0, 1), InPlayerBlockPos.get(0, 0, -1), InPlayerBlockPos.get(1, 0, 0), InPlayerBlockPos.get(-1, 0, 0));

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"),
			method = "getHeight(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F", cancellable = true)
	public void getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfoReturnable<Float> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (pl.getBlocksData2().size() == 1 && pl.getBlocksData2().get(InPlayerBlockPos.ZERO) != null) {
				if (CORNERS.contains(realPos)) {
					cir.setReturnValue(-1f);
				}
			}
		}, null, true);
	}
}
