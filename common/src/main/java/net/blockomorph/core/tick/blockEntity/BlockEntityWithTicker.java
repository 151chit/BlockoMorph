package net.blockomorph.core.tick.blockEntity;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityWithTicker<T extends BlockEntity> implements InPlayerBlockEntityTickManager.BlockEntityInTick {
	public static final InPlayerBlockEntityTickManager.BlockEntityInTick EMPTY = new InPlayerBlockEntityTickManager.BlockEntityInTick() {
		@Override
		public boolean isRemoved() {
			return true;
		}
		@Override
		public void tick() {}
	};
	private final T blockEntity;
	private final BlockEntityTicker<T> ticker;
	private final BlockInPlayer2 block;
	private final InPlayerManager manager;
	private boolean emergencyStop;

	protected BlockEntityWithTicker(InPlayerManager manager, T blockEntity, BlockEntityTicker<T> ticker, BlockInPlayer2 block) {
		this.manager = manager;
		this.blockEntity = blockEntity;
		this.ticker = ticker;
		this.block = block;
	}

	@Override
	public boolean isRemoved() {
		if (this.emergencyStop) return true;
		return this.blockEntity.isRemoved();
	}

	@Override
	public void tick() {
		if (!this.blockEntity.isRemoved() && !this.emergencyStop) {
			BlockState state = this.block.getBlockState();
			if (this.blockEntity.getType().isValid(state)) {
				try {
					this.ticker.tick(this.manager.level(), this.block.getPos(), this.block.getBlockState(), this.blockEntity);
				} catch (Throwable e) {
					MorphUtils.LOGGER.error("An unexpected exception occurred while ticking a block entity in a transformed playerOwner {}: ", this.manager, e);
					this.emergencyStop = true;
				}
			}
		}
	}

	public static class Wrapper implements InPlayerBlockEntityTickManager.BlockEntityInTick {
		private InPlayerBlockEntityTickManager.BlockEntityInTick ticker;

		protected Wrapper(InPlayerBlockEntityTickManager.BlockEntityInTick ticker) {
			this.ticker = ticker;
		}

		public void change(InPlayerBlockEntityTickManager.BlockEntityInTick ticker) {
			this.ticker = ticker;
		}

		@Override
		public boolean isRemoved() {
			return this.ticker.isRemoved();
		}

		@Override
		public void tick() {
			this.ticker.tick();
		}
	}
}
