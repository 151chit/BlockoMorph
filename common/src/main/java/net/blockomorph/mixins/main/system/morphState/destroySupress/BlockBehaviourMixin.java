package net.blockomorph.mixins.main.system.morphState.destroySupress;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockBehaviourMixin {

	@FastInject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z", at = @At("HEAD"))
	public byte isValid(BlockGetter level, BlockPos pos, Direction direction, SupportType supportType) {
		if (direction == Direction.UP) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
			if (posIn != -1 && InPlayerBlockPos.getY(posIn) == 0) return 1;
		}
		return 0;
	}
}
