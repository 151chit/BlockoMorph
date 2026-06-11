package net.blockomorph.utils.accessors;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.Set;

public interface ServerLevelAccessor {
	Set<ResourceKey<DamageType>> formAngGetTntDamages$blockomorph();
}