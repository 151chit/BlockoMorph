package net.blockomorph.mixins.main.system.inPlayerManager.phys.hitbox;

import net.blockomorph.core.HitBoxCalculator;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityDimensions.class)
public class DimensionMixin implements HitBoxCalculator.ManualHitboxEntityDimension {
	@Unique HitBoxCalculator.BlockHitBox hitbox;

	@FastInject(method = "makeBoundingBox(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"))
	public Object makeBoundingBox(double x, double y, double z) {
		if (this.hitbox != null)
			return this.hitbox.makeBox(x, y, z);
		return FastInject.CONTINUE_EXECUTION;
	}

	@Override
	public void overrideCreation(HitBoxCalculator.BlockHitBox hitBox) {
		this.hitbox = hitBox;
	}
}