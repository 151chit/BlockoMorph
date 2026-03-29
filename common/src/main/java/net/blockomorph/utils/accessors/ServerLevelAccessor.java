package net.blockomorph.utils.accessors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.gameEvent.SectionGameEventListenerRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.Set;

public interface ServerLevelAccessor {

	Long2ObjectMap<Int2ObjectMap<SectionGameEventListenerRegistry>> getPlayerGameEventListenerMap();
	Set<ResourceKey<DamageType>> formAngGetTntDamages$blockomorph();
}