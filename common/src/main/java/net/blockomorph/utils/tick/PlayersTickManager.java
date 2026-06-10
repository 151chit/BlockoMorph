package net.blockomorph.utils.tick;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.function.Consumer;

public class PlayersTickManager {
	private static PlayersTicks<Block> BLOCK_TICKS;
	private static PlayersTicks<Fluid> FLUID_TICKS;

	public static void tick() {
		if (Config.getServer() == null) return;
		long gameTime = getGameTime();
		BLOCK_TICKS.tick(gameTime, 65536, (blockPos, block) -> {
			InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
				if (pl.player().level() instanceof ServerLevel lv) {
					BlockState blockState = pl.getBlockState(realPos);
					if (blockState.getBlock() == block) {
						blockState.tick(lv, blockPos, lv.getRandom());
					}
				} else logClientErr(pl);
			}, () -> {
				MorphUtils.LOGGER.error("Attempt to tick a block that is not bound to a player by key position: {} for a blocktype: {}", blockPos, block.getName().getString());
			}, false);
		});
		FLUID_TICKS.tick(gameTime, 65536, (blockPos, fluid) -> {
			InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
				if (pl.player().level() instanceof ServerLevel lv) {
					BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
					if (block != null && block.shouldDoFluidAction()) {
						FluidState fluidState = block.getBlockState().getFluidState();
						if (fluidState.is(fluid)) {
							fluidState.tick(lv, blockPos, block.getBlockState());
						}
					}
				} else logClientErr(pl);
			}, () -> {
				MorphUtils.LOGGER.error("Attempt to tick a fluid that is not bound to a player by key position: {} for a blocktype: {}", blockPos, fluid.getClass().getCanonicalName());
			}, false);
		});
	}

	private static void logClientErr(PlayerAccessor pl) {
		MorphUtils.LOGGER.error("Attempt to tick on non-server level for player {}", pl);
	}

	@SuppressWarnings("unchecked")
	private static <T> PlayersTicks<T> selectCorrectTicker(T type) {
		if (type instanceof Block) {
			return (PlayersTicks<T>) BLOCK_TICKS;
		} else if (type instanceof Fluid) {
			return (PlayersTicks<T>) FLUID_TICKS;
		}
		return null;
	}

	public static <T> void doIfPlayerTick(BlockPos pos, T object, Consumer<PlayersTicks<T>> ticksConsumer, Object ticker) {
		if (!(ticker instanceof PlayersTicks<?>) && InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
			PlayersTicks<T> ticks = selectCorrectTicker(object);
			if (ticks != null)
				ticksConsumer.accept(ticks);
		}
	}

	public static <T> void doIfPlayerTick(ScheduledTick<T> tick, Consumer<PlayersTicks<T>> ticksConsumer, Object ticker) {
		doIfPlayerTick(tick.pos(), tick.type(), ticksConsumer, ticker);
	}

	public static PlayersTicks<?>[] getTicks() {
		return new PlayersTicks[]{BLOCK_TICKS, FLUID_TICKS};
	}

	public static void reinit() {
		BLOCK_TICKS = new PlayersTicks<>("blockTicks", BuiltInRegistries.BLOCK::byNameCodec);
		FLUID_TICKS = new PlayersTicks<>("fluidTicks", BuiltInRegistries.FLUID::byNameCodec);
	}

	public static long getGameTime() {
		return Config.getServer().overworld().getGameTime();
	}

	static {
		reinit();
	}
}
