package net.blockomorph.utils.gameEvent;

import net.blockomorph.mixins.main.level.GameEventRegistryAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulating a vanilla system with double nesting
 */
public class SectionGameEventListenerRegistry extends SafeIterableStorage<SafeIterableStorage<GameEventListener>> {
	private final ServerLevel level;
	private final int sectionY;
	private final EuclideanGameEventListenerRegistry.OnEmptyAction onEmptyAction;

	public SectionGameEventListenerRegistry(ServerLevel lv, int ySection, EuclideanGameEventListenerRegistry.OnEmptyAction action) {
		this.level = lv;
		this.sectionY = ySection;
		this.onEmptyAction = action;
	}

	@Override
	public void remove(SafeIterableStorage<GameEventListener> object) {
		super.remove(object);
		if (this.objects.isEmpty()) {
			this.onEmptyAction.apply(this.sectionY);
		}
	}

	public boolean visitInRangeListeners(Vec3 sourcePos, PlayerDynamicGameEventListener.ListenerVisitorWithHook listenerVisitor) {
		AtomicBoolean finded = new AtomicBoolean();
		this.iterate(storage -> {
			if (listenerVisitor.alreadyUsed().add(storage)) {
				storage.iterate(listener -> {
					Optional<Vec3> optional = GameEventRegistryAccessor.handleGameEvent(this.level, sourcePos, listener);
					if (optional.isPresent()) {
						listenerVisitor.visit(listener, optional.get());
						finded.set(true);
					}
				});
			}
		});
		return finded.get();
	}
}
