package net.blockomorph.core;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.storage.BlocksFastStorage;
import net.blockomorph.core.storage.BlocksInPlayerStorage;

import java.util.ArrayList;

public class DestructionProgressHandler extends BlocksFastStorage<DestructionProgressHandler.BrakeProgress, DestructionProgressHandler.BrakeProgress> {
	private final ArrayList<BrakeProgress> checkForRemove = new ArrayList<>(BlocksInPlayerStorage.ONE_AXIS);
	private final int[] bestBrakesAll = new int[10];
	private int bestProgress = -1;
	private final InPlayerManager manager;

	public DestructionProgressHandler(InPlayerManager manager) {
		super(ignored -> {}, ignored -> {}, wr -> wr, ignored -> true);
		this.manager = manager.assertOnInit();
	}

	public int getBestProgress() {
		return this.bestProgress;
	}

	public int getBestProgress(int pos) {
		BrakeProgress progress = super.get(pos);
		if (progress != null) return progress.bestProgress;
		return -1;
	}

	public void markBlockDestroying(int playerId, int pos, int progressValue) {
		if (InPlayerBlockPos.isBytesInvalid(pos)) return;

		BrakeProgress progress = this.get(pos);
		boolean remove = progressValue < 0 || progressValue >= 10;

		int oldBlockBestProgress = (progress != null) ? progress.bestProgress : -1;

		if (remove) {
			if (progress != null) {
				progress.brakers.remove(playerId);
			}
		} else {
			if (progress == null) {
				progress = new BrakeProgress(pos);
				this.add(progress);
			}
			progress.brakers.put(playerId, progressValue);
		}

		int newBlockBestProgress = -1;
		if (progress != null && !progress.brakers.isEmpty()) {
			IntIterator iter = progress.brakers.values().intIterator();
			while (iter.hasNext()) {
				newBlockBestProgress = Math.max(newBlockBestProgress, iter.nextInt());
			}
			progress.bestProgress = newBlockBestProgress;
			progress.lastUpdate = this.manager.getLifetime();
		} else {
			super.remove(pos);
		}

		if (oldBlockBestProgress != newBlockBestProgress) {
			if (oldBlockBestProgress != -1) this.bestBrakesAll[oldBlockBestProgress]--;
			if (newBlockBestProgress != -1) this.bestBrakesAll[newBlockBestProgress]++;
			this.refreshBestAll();
		}
	}

	protected void tick() {
		if (this.manager.getLifetime() % 20 == 0) {
			super.forEach(this.checkForRemove::add);
			this.checkForRemove.forEach(progress -> {
				if (this.manager.getLifetime() - progress.lastUpdate > 400) {
					this.removeBlock(progress.pos);
				}
			});
			this.checkForRemove.clear();
		}
	}

	protected void removeBlock(int pos) {
		if (InPlayerBlockPos.isBytesInvalid(pos)) return;
		BrakeProgress progress = this.get(pos);
		if (progress != null) {
			this.bestBrakesAll[progress.bestProgress]--;
			super.remove(pos);
			this.refreshBestAll();
		}
	}

	@Override
	public void remove(int inPlayerBlockPos) {
		throw new UnsupportedOperationException();
	}

	private void refreshBestAll() {
		int best = -1;
		for (int i = this.bestBrakesAll.length - 1; i >= 0; i--) {
			if (this.bestBrakesAll[i] > 0) {
				best = i;
				break;
			}
		}
		this.bestProgress = best;
	}

	protected static class BrakeProgress implements InPlayerBlockPosed, StorageWrapper<BrakeProgress> {
		private final int pos;
		private int listIndex;
		private final Int2IntMap brakers = new Int2IntOpenHashMap();
		private int bestProgress = -1;
		private long lastUpdate;

		BrakeProgress(int pos) {
			this.brakers.defaultReturnValue(-1);
			this.pos = pos;
		}

		@Override public int getOffsetAsInt() { return this.pos; }
		@Override public int getIndex() { return this.listIndex; }
		@Override public void setIndex(int i) { this.listIndex = i; }
		@Override public BrakeProgress getElement() { return this; }
	}
}
