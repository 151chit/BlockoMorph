package net.blockomorph.mixins.main.system.morphState.morphAdapt.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

	@FastInject(method = "fall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private static Object fall(Level level, BlockPos pos, BlockState state, @Local FallingBlockEntity entity) {
		int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
		if (posIn == InPlayerBlockPos.ZERO_INT) return entity;
		return FastInject.CONTINUE_EXECUTION;
	}
}
