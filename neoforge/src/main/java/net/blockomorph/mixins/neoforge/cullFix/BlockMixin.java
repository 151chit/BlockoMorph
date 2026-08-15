package net.blockomorph.mixins.neoforge.cullFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Block.class)
public class BlockMixin {

	@ModifyVariable(ordinal = 1, method = "shouldRenderFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", at = @At(value = "HEAD"))
	private static BlockState getBlockStateInPlayer(BlockState state, BlockGetter level, BlockPos pos, @Local(argsOnly = true) Direction direction) {
		if (level instanceof InPlayerBlockAndTintGetter playerProxy && playerProxy.isExternalPos(playerProxy.blockPosHolder().setWithOffset(pos, direction))) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}
}
