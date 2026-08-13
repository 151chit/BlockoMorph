package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.math.MorphDistanceTo;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Vec3i.class)
public class Vec3iMixin {
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int z;

	@ModifyReturnValue(method = "distToCenterSqr(DDD)D", at = @At(value = "RETURN"))
	public double sqr(double original, double x, double y, double z) {
		return MorphDistanceTo.forPoints(original, x, y, z, this.x, this.y, this.z, true, 0.5);
	}

	@ModifyReturnValue(method = "distToLowCornerSqr", at = @At(value = "RETURN"))
	public double sqrCorner(double original, double x, double y, double z) {
		return MorphDistanceTo.forPoints(original, x, y, z, this.x, this.y, this.z, true, 0);
	}
}
