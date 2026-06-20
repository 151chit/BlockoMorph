package net.blockomorph.mixins.main.coords;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.MorphMath;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Vec3.class, priority = 1020)
public class Vec3Mixin {

	@ModifyReturnValue(method = "distanceTo", at = @At(value = "RETURN"))
	public double getRealCoords(double original, Vec3 vec3) {
		return MorphMath.distanceTo(original, vec3, (Vec3) (Object) this, false, 0);
	}

	@ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At(value = "RETURN"))
	public double getRealCoordsSqr(double original, Vec3 vec3) {
		return MorphMath.distanceTo(original, vec3, (Vec3) (Object) this, true, 0);
	}

	@ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At(value = "RETURN"))
	public double getRealCoordsSqrDDD(double original, double x, double y, double z) {
		return MorphMath.distanceTo(original, (Vec3) (Object) this, x, y, z, true, 0);
	}
}
