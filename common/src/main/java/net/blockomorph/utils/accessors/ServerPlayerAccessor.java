package net.blockomorph.utils.accessors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

public interface ServerPlayerAccessor {
	void dropAllDeathLoot$blockomorph(ServerLevel lv, DamageSource dm);
}