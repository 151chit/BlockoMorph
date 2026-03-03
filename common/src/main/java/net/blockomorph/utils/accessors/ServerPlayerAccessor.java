package net.blockomorph.utils.accessors;

import net.minecraft.world.damagesource.DamageSource;

public interface ServerPlayerAccessor {
	void dropAllDeathLoot$blockomorph(DamageSource dm);
	boolean isDeadAndReset$blockomorph();
}