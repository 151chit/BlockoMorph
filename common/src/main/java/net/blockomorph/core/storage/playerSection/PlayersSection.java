package net.blockomorph.core.storage.playerSection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import oshi.annotation.concurrent.NotThreadSafe;

import java.util.*;
import java.util.function.IntConsumer;

@NotThreadSafe
public class PlayersSection {
	private final Object2ObjectOpenHashMap<UUID, PlayerWrapper> players = new Object2ObjectOpenHashMap<>(16);
	private final ArrayList<PlayerWrapper> iterPlayers = new ArrayList<>(16);

	private final IntArrayList iterStack = new IntArrayList(16);
	private final IntArrayList removed = new IntArrayList(16);
	private int stackDepth = 0;
	private boolean iteration;
	private final SectionPos pos;
	private final IntConsumer onEmpty;
	private boolean empty;

	protected PlayersSection(SectionPos pos, IntConsumer onEmpty) {
		this.pos = pos;
		this.onEmpty = onEmpty;
	}

	public SectionPos getPos() {
		return this.pos;
	}

	public boolean isEmpty() {
		return this.empty;
	}

	protected void add(Player player) {
		this.empty = false;
		var oldWrapper = this.players.get(player.getUUID());
		if (oldWrapper != null) {
			oldWrapper.player = player;
			return;
		}
		PlayerWrapper wrapper = new PlayerWrapper(player);
		this.players.put(player.getUUID(), wrapper);
		this.iterPlayers.add(wrapper);
		wrapper.listIndex = this.iterPlayers.size() - 1;
	}

	protected void remove(UUID uuid) {
		var wrapper = this.players.remove(uuid);
		if (wrapper == null) return;

		int removingPos = wrapper.listIndex;
		int lastListIndex = this.iterPlayers.size() - 1;

		if (this.stackDepth > 0) {
			this.iterPlayers.set(removingPos, null);
			this.removed.add(removingPos);
			return;
		}

		if (removingPos == lastListIndex) {
			this.iterPlayers.remove(lastListIndex);
		} else {
			var oldWrapper = this.iterPlayers.get(lastListIndex);
			this.iterPlayers.set(removingPos, oldWrapper);
			oldWrapper.listIndex = removingPos;
			this.iterPlayers.remove(lastListIndex);
		}
		if (this.iterPlayers.size() - this.removed.size() <= 0) {
			this.empty = true;
			this.onEmpty.accept(this.pos.y());
		}
	}

	protected void beginAccess() {
		if (this.stackDepth == 0) {
			if (this.iteration) throw new IllegalStateException();
			this.iteration = true;
		}
		int currentSize = this.iterPlayers.size();

		if (this.stackDepth >= this.iterStack.size()) {
			this.iterStack.add(currentSize - 1);
		} else {
			this.iterStack.set(this.stackDepth, currentSize - 1);
		}

		this.stackDepth++;
	}

	public boolean hasNext() {
		int currentLevel = this.stackDepth - 1;
		int cursor = this.iterStack.getInt(currentLevel);

		while (cursor >= 0) {
			if (this.iterPlayers.get(cursor) != null) {
				this.iterStack.set(currentLevel, cursor);
				return true;
			}
			cursor--;
		}
		return false;
	}

	public Player next() {
		int currentLevel = this.stackDepth - 1;
		int cursor = this.iterStack.getInt(currentLevel);

		Player player = this.iterPlayers.get(cursor).player;

		this.iterStack.set(currentLevel, cursor - 1);
		return player;
	}

	public void afterAccess() {
		this.stackDepth--;
		if (this.stackDepth < 0) {
			throw new IllegalStateException();
		} else if (this.stackDepth == 0 && !this.removed.isEmpty()) {
			this.removeHoles();
		}
		this.iteration = false;
	}

	private void removeHoles() {
		for (int i = 0; i < this.removed.size(); i++) {
			int removedPos = this.removed.getInt(i);
			if (removedPos >= this.iterPlayers.size()) continue;
			PlayerWrapper tail = this.tryFindTail(this.iterPlayers.size() - 1);
			if (tail != null) {
				int tailIndex = tail.listIndex;
				if (removedPos > tailIndex) continue;
				this.iterPlayers.set(removedPos, tail);
				tail.listIndex = removedPos;
				this.iterPlayers.remove(tailIndex);
			} else {

				break;
			}
		}
		this.removed.clear();
	}

	private PlayerWrapper tryFindTail(int tail) {
		if (tail < 0) return null;
		PlayerWrapper wrapper = this.iterPlayers.get(tail);
		if (wrapper == null) {
			this.iterPlayers.remove(tail);
			return this.tryFindTail(tail - 1);
		}
		return wrapper;
	}

	private static class PlayerWrapper {
		Player player;
		int listIndex = -1;

		private PlayerWrapper(Player player) {
			this.player = player;
		}
	}
}
