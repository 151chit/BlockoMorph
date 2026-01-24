package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.tick.PlayersTickManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> {

	@Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
	private void schedule(ScheduledTick<T> scheduledTick, CallbackInfo ci) {
		PlayersTickManager.doIfPlayerTick(scheduledTick, ticker -> {
			ticker.schedule(scheduledTick);
			ci.cancel();
		}, this);
	}

	@Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
	private void hasScheduledTick(BlockPos blockPos, T object, CallbackInfoReturnable<Boolean> cir) {
		PlayersTickManager.doIfPlayerTick(blockPos, object, ticker -> {
			cir.setReturnValue(ticker.hasScheduledTick(blockPos, object));
		}, this);
	}

	@Inject(method = "willTickThisTick", at = @At("HEAD"), cancellable = true)
	private void willTickThisTick(BlockPos blockPos, T object, CallbackInfoReturnable<Boolean> cir) {
		PlayersTickManager.doIfPlayerTick(blockPos, object, ticker -> {
			cir.setReturnValue(ticker.willTickThisTick(blockPos, object));
		}, this);
	}
}
