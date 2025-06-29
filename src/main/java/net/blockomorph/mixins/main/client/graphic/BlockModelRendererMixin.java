package net.blockomorph.mixins.main.client.graphic;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = ModelBlockRenderer.class, priority = 30000)
public class BlockModelRendererMixin {

	@WrapOperation(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	public BlockState run(BlockAndTintGetter instance, BlockPos blockPos, Operation<BlockState> original) {
		AtomicReference<BlockState> state = new AtomicReference<>(original.call(instance, blockPos));
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> state.set(pl.getBlockState(realPos)), null, true);
		return state.get();
	}
}
