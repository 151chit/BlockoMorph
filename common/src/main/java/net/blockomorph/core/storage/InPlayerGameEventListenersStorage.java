package net.blockomorph.core.storage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.mixins.main.rawAccessors.GameEventRegistryAcc;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Vanilla copy for dynamic level getting by manager and remove debug subscribes */
public class InPlayerGameEventListenersStorage implements GameEventListenerRegistry {
	private final List<GameEventListener> listeners = new ObjectArrayList<>();
	private final Set<GameEventListener> listenersToRemove = new ObjectOpenHashSet<>();
	private final List<GameEventListener> listenersToAdd = new ObjectArrayList<>();
	private boolean processing;
	private final InPlayerManager manager;
	public InPlayerGameEventListenersStorage(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
	}

	@Override
	public boolean isEmpty() {
		return this.listeners.isEmpty();
	}

	@Override
	public void register(GameEventListener listener) {
		if (this.processing) {
			this.listenersToAdd.add(listener);
		} else {
			this.listeners.add(listener);
		}
	}

	@Override
	public void unregister(GameEventListener listener) {
		if (this.processing) {
			this.listenersToRemove.add(listener);
		} else {
			this.listeners.remove(listener);
		}
	}

	@Override
	public boolean visitInRangeListeners(Holder<GameEvent> event, Vec3 sourcePosition, GameEvent.Context context, ListenerVisitor action) {
		if (this.manager.level() instanceof ServerLevel lv) {
			if (this.processing) throw new ConcurrentModificationException();
			if (this.isEmpty()) return false;
			this.processing = true;
			boolean applicable = false;

			try {
				Iterator<GameEventListener> iterator = this.listeners.iterator();
				while (iterator.hasNext()) {
					GameEventListener listener = iterator.next();
					if (this.listenersToRemove.remove(listener)) {
						iterator.remove();
					} else {
						Optional<Vec3> optionalPosition = GameEventRegistryAcc.handleGameEvent(lv, sourcePosition, listener);
						if (optionalPosition.isPresent()) {
							action.visit(listener, optionalPosition.get());
							applicable = true;
						}
					}
				}
			} finally {
				this.processing = false;
			}

			if (!this.listenersToAdd.isEmpty()) {
				this.listeners.addAll(this.listenersToAdd);
				this.listenersToAdd.clear();
			}

			if (!this.listenersToRemove.isEmpty()) {
				this.listeners.removeAll(this.listenersToRemove);
				this.listenersToRemove.clear();
			}

			return applicable;
		}
		return false;
	}

	public static boolean processGameEvents(ServerLevel level, Holder<GameEvent> event, Vec3 sourcePosition, GameEvent.Context context,
			GameEventListenerRegistry.ListenerVisitor action, int sectionMinX, int sectionMinY, int sectionMinZ, int sectionMaxX, int sectionMaxY, int sectionMaxZ) {
		boolean used = false;
		PlayersStorage storage = PlayersStorage.ofLevel(level);
		try (storage) {
			for (Player player : storage.findMorphedPlayers(null,
					SectionPos.sectionToBlockCoord(sectionMinX),
					SectionPos.sectionToBlockCoord(sectionMinY),
					SectionPos.sectionToBlockCoord(sectionMinZ),
					SectionPos.sectionToBlockCoord(sectionMaxX, 15),
					SectionPos.sectionToBlockCoord(sectionMaxY, 15),
					SectionPos.sectionToBlockCoord(sectionMaxZ, 15))) {
				used |= PlayerAccessor.of(player).getEventListenersStorage().visitInRangeListeners(event, sourcePosition, context, action);
			}
		}
		return used;
	}
}
