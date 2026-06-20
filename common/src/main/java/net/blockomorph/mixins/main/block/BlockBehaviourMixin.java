package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockBehaviourMixin {

	@Inject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z", at = @At("HEAD"), cancellable = true)
	public void isValid(BlockGetter level, BlockPos pos, Direction dir, SupportType p_60663_, CallbackInfoReturnable<Boolean> cir) {
		if (dir == Direction.UP) {
			InPlayerBlockPos morphPos = InPlayerBlockPos.getBlockPosInPlayer(pos);
			if (morphPos != null && morphPos.getY() == -1) cir.setReturnValue(true);
		}
	}
}
