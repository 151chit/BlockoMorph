package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

	@Inject(method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"), cancellable = true)
	private static void isValid(BlockState p_49909_, BlockState p_49910_, LevelAccessor lv, BlockPos pos, int p_49913_, int p_49914_, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (realPos.getY() == 0 && pl.isOnLoadingBlocks()) {
				ci.cancel();
			}
		}, ci::cancel, lv);
	}

	@ModifyVariable(method = "shouldRenderFace", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getRealState(BlockState neighbour, BlockState parent, BlockGetter lv, BlockPos parentPos, Direction dirToFace, BlockPos neighbourPos) {
		return MorphUtils.lockExternalMorphedGetter(neighbour, lv instanceof LevelReader lr ? lr : null, neighbourPos);
	}
}
