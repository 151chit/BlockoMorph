package net.blockomorph.mixins.neoforge.cullFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getBlockStateInPlayer(BlockState blockstate, BlockAndTintGetter level, @Local(ordinal = 1, argsOnly = true) BlockPos neighborPos) {
		if (level instanceof InPlayerBlockAndTintGetter playerProxy) {
			if (playerProxy.isExternalPos(neighborPos))
				return Blocks.AIR.defaultBlockState();
		}
		return blockstate;
	}
}
