package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.math.MorphDistanceTo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {

	@ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At(value = "RETURN"))
	public double getRealCoordsSqr(double original, double x, double y, double z) {
		return MorphDistanceTo.forPoints(original, ((Entity) (Object) this).position(), x, y, z, true, 0);
	}

	@ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"))
	public double getRealCoordsVec3(double original, Vec3 pos) {
		return MorphDistanceTo.forPoints(original, ((Entity) (Object) this).position(), pos, true, 0);
	}
}
