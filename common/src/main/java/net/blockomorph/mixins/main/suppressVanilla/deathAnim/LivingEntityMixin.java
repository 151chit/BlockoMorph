package net.blockomorph.mixins.main.suppressVanilla.deathAnim;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	public abstract void remove(Entity.RemovalReason reason);

	@FastInject(method = "tickDeath", at = @At("HEAD"))
	private boolean suppress() {
		if (this instanceof PlayerAccessor pl && pl.isBlockomorphActive()) {
			this.remove(Entity.RemovalReason.KILLED);
			return false;
		}
		return true;
	}
}
