package net.blockomorph.mixins.main.system.morphState.deathPrepare;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

	@ModifyVariable(method = "destroyBlock", at = @At(value = "STORE"))
	public FluidState crackBlockStart(FluidState value, BlockPos pos) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null)
			pl.markBreakingStart(true);
		return value;
	}

	@Inject(method = "destroyBlock", at = @At(value = "RETURN", ordinal = 4))
	public void crackBlockEnd(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null)
			pl.markBreakingStart(false);
	}
}