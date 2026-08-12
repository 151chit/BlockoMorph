package net.blockomorph.core.storage;

import net.blockomorph.core.coords.InPlayerBlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
public class BlocksFastStorage<T extends BlocksFastStorage.InPlayerBlockPosed, WRAPPER extends BlocksFastStorage.StorageWrapper<T>> implements Iterable<T> {
	public static final int ONE_AXIS = 32;
	public static final int SIZE = ONE_AXIS * ONE_AXIS * ONE_AXIS;
	private final ReusableIterator iterator = new ReusableIterator();
	private final Object[] coordsMatrix = new Object[SIZE];
	private final ArrayList<WRAPPER> iteratingList = new ArrayList<>(SIZE);
	private final Consumer<T> addCallback, removeCallback;
	private final Function<T, WRAPPER> factory;
	private final Predicate<T> preAddCheck;

	public BlocksFastStorage(Consumer<T> addCallback, Consumer<T> removeCallback, Function<T, WRAPPER> factory, Predicate<T> preAddCheck) {
		this.factory = factory;
		this.addCallback = addCallback;
		this.removeCallback = removeCallback;
		this.preAddCheck = preAddCheck;
	}

	@Nullable
	public T get(int inPlayerBlockPos) {
		if (InPlayerBlockPos.isBytesInvalid(inPlayerBlockPos)) return null;
		WRAPPER wrapper = (WRAPPER) this.coordsMatrix[inPlayerBlockPos];
		if (wrapper == null) return null;
		return wrapper.getElement();
	}

	@Nullable
	public T getFirst() {
		if (this.iteratingList.isEmpty()) return null;
		return this.iteratingList.getFirst().getElement();
	}

	public int size() {
		return this.iteratingList.size();
	}

	public void add(T block) {
		this.checkMods("add");
		if (!this.preAddCheck.test(block)) return;
		int index = block.getOffsetAsInt();
		WRAPPER oldBlock = (WRAPPER) this.coordsMatrix[index];
		if (oldBlock != null) {
			if (oldBlock.getElement() == block) return;
			this.remove(index);
		}
		WRAPPER blockWrapper = this.factory.apply(block);
		this.coordsMatrix[index] = blockWrapper;
		this.iteratingList.add(blockWrapper);
		blockWrapper.setIndex(this.iteratingList.size() - 1);
		this.addCallback.accept(block);
	}

	public void remove(int inPlayerBlockPos) {
		if (this.size() == 0 || InPlayerBlockPos.isBytesInvalid(inPlayerBlockPos)) return;
		this.checkMods("remove");
		WRAPPER block = (WRAPPER) this.coordsMatrix[inPlayerBlockPos];
		if (block != null) {
			int indexInListForRemove = block.getIndex();
			this.coordsMatrix[inPlayerBlockPos] = null;
			int lastListIndex = this.iteratingList.size() - 1;
			if (lastListIndex == indexInListForRemove) {
				this.iteratingList.remove(lastListIndex);
			} else {
				WRAPPER last = this.iteratingList.get(lastListIndex);
				this.iteratingList.set(indexInListForRemove, last);
				last.setIndex(indexInListForRemove);
				this.iteratingList.remove(lastListIndex);
			}
			this.removeCallback.accept(block.getElement());
		}
	}

	/**
	 * DO NOT USE "continue" or "break", terminate iterator manually instead
	 */
	@Override
	public Iterator<T> iterator() {
		this.checkMods("iterate");
		this.iterator.locked = true;
		return this.iterator;
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		if (this.size() == 0) return;
		this.checkMods("forEach");
		this.iterator.locked = true;
		try {
			int size = this.iteratingList.size();
			for (int i = 0; i < size; i++) {
				action.accept(this.iteratingList.get(i).getElement());
			}
		} finally {
			this.releaseIterator();
		}
	}

	@Override
	public Spliterator<T> spliterator() {
		throw new UnsupportedOperationException();
	}

	public void releaseIterator() {
		this.iterator.close();
	}

	private void checkMods(String reason) {
		if (this.iterator.locked) throw new ConcurrentModificationException(reason);
	}

	private class ReusableIterator implements Iterator<T>, AutoCloseable {
		private boolean locked;
		private int cursor = 0;

		public void close() {
			this.cursor = 0;
			this.locked = false;
		}

		@Override
		public boolean hasNext() {
			boolean hasElements = this.cursor < iteratingList.size();
			if (!hasElements) {
				this.close();
			}
			return hasElements;
		}

		@Override
		public T next() {
			if (!this.hasNext()) throw new NoSuchElementException();
			return iteratingList.get(this.cursor++).getElement();
		}
	}

	public interface InPlayerBlockPosed {
		int getOffsetAsInt();
	}

	public interface StorageWrapper<T extends InPlayerBlockPosed> {
		int getIndex();
		void setIndex(int i);
		T getElement();
	}
}
