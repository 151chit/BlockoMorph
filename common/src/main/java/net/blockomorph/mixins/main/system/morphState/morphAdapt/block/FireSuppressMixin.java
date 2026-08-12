package net.blockomorph.mixins.main.system.morphState.morphAdapt.block;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public class FireSuppressMixin {

	@FastInject(method = "tick", at = @At("HEAD"))
	public boolean run(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		return !InPlayerBlockPos.isMorphedPlayerBlockX(pos.getX());
	}

}
