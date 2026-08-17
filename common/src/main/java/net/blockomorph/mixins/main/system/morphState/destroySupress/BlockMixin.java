package net.blockomorph.mixins.main.system.morphState.destroySupress;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public class BlockMixin {

	@FastInject(method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"))
	private static boolean isValid(BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, int updateFlags, int updateLimit) {
		int posIn = InPlayerBlockPos.findInPlayerBlockPos(blockPos);
		if (posIn != -1 && InPlayerBlockPos.getY(posIn) == 0) {
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(blockPos);
			return pl == null || !pl.getManager().getFlags().morphProcess.isTrue();
		}
		return true;
	}
}
