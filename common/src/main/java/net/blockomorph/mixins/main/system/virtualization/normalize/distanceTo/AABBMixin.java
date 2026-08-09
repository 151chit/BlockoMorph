package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.math.MorphDistanceTo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AABB.class)
public class AABBMixin {

	@ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"))
	private double normPoint(double original, Vec3 point) {
		return MorphDistanceTo.forAabbAndPointSqr(original, this.getThis(), point);
	}

	@ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/AABB;)D", at = @At("RETURN"))
	private double normAABB(double original, AABB boundingBox) {
		return MorphDistanceTo.forAabbAndAabbSqr(original, this.getThis(), boundingBox);
	}

	@Unique
	private AABB getThis() {
		return (AABB) (Object) this;
	}
}