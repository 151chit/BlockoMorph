package net.blockomorph.utils.gameEvent;

import java.util.*;
import java.util.function.Consumer;

public class SafeIterableStorage<T> {
	
	protected final List<T> objects = new ArrayList<>();
	protected final Set<T> objectsToRemove = new HashSet<>();
	protected final List<T> objectsToAdd = new ArrayList<>();
	private boolean iterating;
	
	public void add(T object) {
		if (this.iterating) {
			this.objectsToAdd.add(object);
		} else {
			this.objects.add(object);
		}
	}

	public void remove(T object) {
		if (this.iterating) {
			this.objectsToRemove.add(object);
		} else {
			this.objects.remove(object);
		}
	}
	
	public void iterate(Consumer<T> objectConsumer) {
		this.iterating = true;

		try {
			Iterator<T> iterator = this.objects.iterator();
			while(iterator.hasNext()) {
				T object = iterator.next();
				if (this.objectsToRemove.remove(object)) {
					iterator.remove();
				} else {
					objectConsumer.accept(object);
				}
			}
		} finally {
			this.iterating = false;
		}

		if (!this.objectsToAdd.isEmpty()) {
			this.objects.addAll(this.objectsToAdd);
			this.objectsToAdd.clear();
		}

		if (!this.objectsToRemove.isEmpty()) {
			this.objects.removeAll(this.objectsToRemove);
			this.objectsToRemove.clear();
		}
	}
}
