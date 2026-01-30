package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(method = "shouldRenderFace", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getBlockStateInPlayer(BlockState original, BlockAndTintGetter blockAndTintGetter, BlockState blockState, boolean bl, Direction direction, BlockPos blockPos) {
		AtomicReference<BlockState> state = new AtomicReference<>(original);
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> state.set(pl.getBlockState(realPos)), null, true);
		return state.get();
	}
}
