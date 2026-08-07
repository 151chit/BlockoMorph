package net.blockomorph.core.storage;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;

import java.util.Iterator;
import java.util.function.Consumer;

public class BlocksInPlayerStorage extends BlocksFastStorage<BlockInPlayer2, BlocksInPlayerStorage.BlockWrapper> {
	public static final BlocksInPlayerStorage BLACKHOLE = new Empty();

	public BlocksInPlayerStorage(InPlayerManager manager, Consumer<BlockInPlayer2> addCallback, Consumer<BlockInPlayer2> removeCallback) {
		manager.assertOnInit();
		super(addCallback, removeCallback, BlockWrapper::new, block -> {
			if (block.getBlockState().isAir()) return false;
			if (InPlayerBlockPos.isInvalidPosFor(manager, block.getPos()))
				throw new IllegalArgumentException("Adding block for someone else's playerOwner, block: " + block.getPlayer() + " Owner: " + manager.getOwner());
			return true;
		});
	}

	private BlocksInPlayerStorage() {
		super(null, null, null, null);
	}

	public BlockInPlayer2 randomSortedBlockOrThrow() {
		BlockInPlayer2 block = this.getFirst();
		if (block != null) return block;
		throw new UnsupportedOperationException("Requested single block, but size == 0");
	}

	protected static class BlockWrapper implements StorageWrapper<BlockInPlayer2> {
		private int indexInList;
		private final BlockInPlayer2 block;
		private BlockWrapper(BlockInPlayer2 block) {
			this.block = block;
		}

		@Override public int getIndex() { return this.indexInList; }
		@Override public void setIndex(int i) { this.indexInList = i; }
		@Override public BlockInPlayer2 getElement() { return this.block; }
	}

	private static class Empty extends BlocksInPlayerStorage {
		private static final Iterator<BlockInPlayer2> EMPTY = new Iterator<>() {
			@Override public boolean hasNext() { return false; }
			@Override public BlockInPlayer2 next() { return null; }
		};
		@Override public BlockInPlayer2 get(int inPlayerBlockPos) { return null; }
		@Override public int size() { return 0; }
		@Override public BlockInPlayer2 getFirst() { return null; }
		@Override public void add(BlockInPlayer2 block) {}
		@Override public void remove(int inPlayerBlockPos) {}
		@Override public void forEach(Consumer<? super BlockInPlayer2> action) {}
		@Override public Iterator<BlockInPlayer2> iterator() { return EMPTY; }
		@Override public BlockInPlayer2 randomSortedBlockOrThrow() { throw new UnsupportedOperationException("Storage is empty!"); }
	}
}
