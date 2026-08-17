package net.blockomorph.core.serialization;

import com.google.common.collect.AbstractIterator;
import net.minecraft.nbt.*;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;

@SuppressWarnings("unchecked")
public interface DataWorker {
	void tick();
	boolean isDone();
	void run();

	static Iterable<String> keysCompound(CompoundTag tag) {
		return tag.getAllKeys();
	}

	static Iterable<Tag> valuesCompound(CompoundTag tags) {
		return () -> new AbstractIterator<>() {
			final Iterator<String> keysIterator = tags.getAllKeys().iterator();

			@Override
			protected Tag computeNext() {
				if (!keysIterator.hasNext()) {
					return this.endOfData();
				}
				return tags.get(keysIterator.next());
			}
		};
	}

	static <T extends Tag> Optional<T> getTagFrom(@Nullable CompoundTag tag, String name, TagType<T> type) {
		if (tag == null) return Optional.empty();
		Tag containsTag = tag.get(name);
		if (containsTag != null && containsTag.getType() == type) {
			return Optional.of((T) containsTag);
		}
		return Optional.empty();
	}

	@SuppressWarnings("ConstantConditions")
	static <T extends Tag> Optional<T> getTagFrom(@Nullable CollectionTag<?> tag, int number, TagType<T> type) {
		if (tag == null) return Optional.empty();
		if (number < 0 || number >= tag.size()) return Optional.empty();
		Tag containsTag = tag.get(number);
		if (containsTag != null && containsTag.getType() == type) {
			return Optional.of((T) containsTag);
		}
		return Optional.empty();
	}
}
