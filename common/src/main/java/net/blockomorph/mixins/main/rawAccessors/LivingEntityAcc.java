package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAcc extends Entity implements Accessors.LivingEntityAccessor {
	public LivingEntityAcc(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Shadow protected abstract void dropAllDeathLoot(ServerLevel level, DamageSource source);
	@Override
	public void dropAllLoot$bm(DamageSource damage) {
		if (this.level() instanceof ServerLevel lv) {
			this.dropAllDeathLoot(lv, damage);
		}
		throw new UnsupportedOperationException("called on client!");
	}
}
