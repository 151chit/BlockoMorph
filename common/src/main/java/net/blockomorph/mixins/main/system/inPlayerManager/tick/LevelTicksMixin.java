package net.blockomorph.mixins.main.system.inPlayerManager.tick;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.tick.PlayerTicks;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> {

	@FastInject(method = "schedule", at = @At("HEAD"))
	private boolean schedule(ScheduledTick<T> scheduledTick) {
		PlayerTicks<T> ticks = this.selectGoodTicks(scheduledTick.pos(), scheduledTick.type());
		if (ticks != null) {
			ticks.schedule(scheduledTick);
			return false;
		}
		return true;
	}

	@FastInject(method = "hasScheduledTick", at = @At("HEAD"))
	private byte hasScheduledTick(BlockPos blockPos, T object) {
		PlayerTicks<T> ticks = this.selectGoodTicks(blockPos, object);
		if (ticks != null) {
			return (byte) (ticks.hasScheduledTick(blockPos, object) ? 1 : -1);
		}
		return 0;
	}

	@FastInject(method = "willTickThisTick", at = @At("HEAD"))
	private byte willTickThisTick(BlockPos blockPos, T object) {
		PlayerTicks<T> ticks = this.selectGoodTicks(blockPos, object);
		if (ticks != null) {
			return (byte) (ticks.willTickThisTick(blockPos, object) ? 1 : -1);
		}
		return 0;
	}

	@Unique
	private <U> PlayerTicks<U> selectGoodTicks(BlockPos pos, U tickType) {
		if ((Object)this instanceof PlayerTicks<?>) return null;
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null && !pl.isNotInitialized()) return pl.getTicksForElementType(tickType);
		return null;
	}
}
