package net.blockomorph.core.tick.blockEntity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * This class is needed to simulate vanilla block entity baseTick logic, similar to LevelChunk. Iterating over BlockInPlayer2 causes arbitrary ticks to fire, which corrupts the piston logic.
 */
public class InPlayerBlockEntityTickManager {
	private final InPlayerManager manager;
	private final List<BlockEntityInTick> ticking = new ArrayList<>();
	private final List<BlockEntityInTick> pendingTick = new ArrayList<>();
	private final Int2ObjectMap<BlockEntityWithTicker.Wrapper> tickers = new Int2ObjectOpenHashMap<>();

	private boolean tickStarted;

	public InPlayerBlockEntityTickManager(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
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
		BlockEntityWithTicker.Wrapper wrapper = this.tickers.remove(pos.asInt());
		if (wrapper != null) {
			wrapper.change(BlockEntityWithTicker.EMPTY);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> void updateBlockEntityTicker(BlockInPlayer2 block) {
		if (block.getSectionId() != this.manager.getSectionId())
			throw new IllegalArgumentException("Try to register blockEntityTicker to other playerOwner! " + block.getSectionId() + " " + this.manager.getSectionId());
		T blockEntity = (T) block.getBlockEntity();
		BlockState blockState = blockEntity.getBlockState();
		BlockEntityTicker<T> blockEntityTicker = (BlockEntityTicker<T>) blockState.getTicker(this.manager.level(), blockEntity.getType());
		if (blockEntityTicker == null) {
			this.removeBlockEntityTicker(block.getOffset());
		} else {
			BlockEntityWithTicker.Wrapper old = this.tickers.get(block.getOffset().asInt());
			this.tickers.put(block.getOffset().asInt(), this.createNewWrapperIfNeed(old, blockEntityTicker, blockEntity, block));
		}
	}

	private <T extends BlockEntity> BlockEntityWithTicker.Wrapper createNewWrapperIfNeed(BlockEntityWithTicker.Wrapper wrapper, BlockEntityTicker<T> blockEntityTicker, T blockentity, BlockInPlayer2 block) {
		BlockEntityInTick tickingBlockEntity = new BlockEntityWithTicker<>(this.manager, blockentity, blockEntityTicker, block);
		if (wrapper != null) {
			wrapper.change(tickingBlockEntity);
			return wrapper;
		} else {
			BlockEntityWithTicker.Wrapper wrapper1 = new BlockEntityWithTicker.Wrapper(tickingBlockEntity);
			this.add(wrapper1);
			return wrapper1;
		}
	}

	public interface BlockEntityInTick {
		boolean isRemoved();
		void tick();
	}
}
