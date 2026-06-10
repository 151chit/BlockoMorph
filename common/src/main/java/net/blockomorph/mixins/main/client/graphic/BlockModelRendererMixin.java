package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getBlockStateInPlayer(BlockState neighborState, final BlockAndTintGetter level, final BlockState state2, final Direction direction, final BlockPos neighborPos) {
		AtomicReference<BlockState> state = new AtomicReference<>(neighborState);
		InPlayerBlockPos.check(neighborPos, (pl, realPos) -> state.set(pl.getBlockState(realPos)), null, true);
		return state.get();
	}
}
