package net.blockomorph.mixins.neoforge.cullFix;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getBlockStateInPlayer(BlockState neighborState, BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos neighborPos) {
		if (level instanceof InPlayerBlockAndTintGetter playerProxy) {
			if (playerProxy.isExternalPos(neighborPos))
				return Blocks.AIR.defaultBlockState();
		}
		return neighborState;
	}
}
