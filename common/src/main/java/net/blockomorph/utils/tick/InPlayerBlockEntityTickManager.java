package net.blockomorph.utils.tick;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * This class is needed to simulate vanilla block entity tick logic, similar to LevelChunk. Iterating over BlockInPlayer2 causes arbitrary ticks to fire, which corrupts the piston logic.
 */
public class InPlayerBlockEntityTickManager {
	private final PlayerAccessor owner;
	private final List<BlockEntityInTick> ticking = new ArrayList<>();
	private final List<BlockEntityInTick> pendingTick = new ArrayList<>();
	private final Map<InPlayerBlockPos, BlockEntityWithTicker.Wrapper> tickers = new HashMap<>();

	private boolean tickStarted;

	public InPlayerBlockEntityTickManager(PlayerAccessor pl) {
		this.owner = pl;
	}

	public void tick() {
		this.tickStarted = true;

		if (!this.pendingTick.isEmpty()) {
			this.ticking.addAll(this.pendingTick);
			this.pendingTick.clear();
		}

		Iterator<BlockEntityInTick> tickingIterator = this.ticking.iterator();
		while (tickingIterator.hasNext()) {
			BlockEntityInTick blockEntityInTick = tickingIterator.next();
			if (blockEntityInTick.isRemoved()) {
				tickingIterator.remove();
			} else blockEntityInTick.tick();
		}

		this.tickStarted = false;
	}

	private void add(BlockEntityInTick blockEntityInTick) {
		if (this.tickStarted) {
			this.pendingTick.add(blockEntityInTick);
		} else {
			this.ticking.add(blockEntityInTick);
		}
	}

	public void removeBlockEntityTicker(InPlayerBlockPos pos) {
		BlockEntityWithTicker.Wrapper wrapper = this.tickers.remove(pos);
		if (wrapper != null) {
			wrapper.change(BlockEntityWithTicker.EMPTY);
		}
	}

	@SuppressWarnings("unchecked")
	public  <T extends BlockEntity> void updateBlockEntityTicker(BlockInPlayer2 block) {
		T blockEntity = (T) block.getBlockEntity();
		BlockState blockState = blockEntity.getBlockState();
		BlockEntityTicker<T> blockEntityTicker = (BlockEntityTicker<T>) blockState.getTicker(this.owner.player().level(), blockEntity.getType());
		if (blockEntityTicker == null) {
			this.removeBlockEntityTicker(block.getOffset());
		} else {
			this.tickers.compute(block.getOffset(), (pos, wrapper) -> {
				BlockEntityInTick tickingBlockEntity = new BlockEntityWithTicker<>(blockEntity, blockEntityTicker, this.owner, block);
				if (wrapper != null) {
					wrapper.change(tickingBlockEntity);
					return wrapper;
				} else {
					BlockEntityWithTicker.Wrapper wrapper1 = new BlockEntityWithTicker.Wrapper(tickingBlockEntity);
					this.add(wrapper1);
					return wrapper1;
				}
			});
		}
	}

	public interface BlockEntityInTick {
		boolean isRemoved();
		void tick();
	}
}
