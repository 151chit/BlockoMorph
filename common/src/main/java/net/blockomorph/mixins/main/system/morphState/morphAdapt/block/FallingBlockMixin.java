package net.blockomorph.mixins.main.system.morphState.morphAdapt.block;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public class FallingBlockMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(pos.getX())) {
			ci.cancel();
		}
	}
}
