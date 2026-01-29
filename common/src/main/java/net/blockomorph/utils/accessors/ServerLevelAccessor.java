package net.blockomorph.utils.accessors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.gameEvent.SectionGameEventListenerRegistry;

public interface ServerLevelAccessor {

	Long2ObjectMap<Int2ObjectMap<SectionGameEventListenerRegistry>> getPlayerGameEventListenerMap();
}