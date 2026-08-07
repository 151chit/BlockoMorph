package net.blockomorph.mixins.main.system.inPlayerManager.damageHandler;

import net.blockomorph.core.misc.DamageHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements LevelReader, DamageHandler.TntFuseDamagesProvider {
	@Unique private Set<ResourceKey<DamageType>> allowedDamages;

	@Override
	public Set<ResourceKey<DamageType>> makeOrGet$blockomorph() {
		if (this.allowedDamages == null) {
			Set<ResourceKey<DamageType>> damages = this.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).entrySet().stream().filter(entry ->
				entry.getValue().effects() == DamageEffects.BURNING
			).map(Map.Entry::getKey).collect(Collectors.toSet());
			damages.addAll(DamageHandler.TNT_DAMAGE);
			this.allowedDamages = Collections.unmodifiableSet(damages);
		}
		return this.allowedDamages;
	}

}