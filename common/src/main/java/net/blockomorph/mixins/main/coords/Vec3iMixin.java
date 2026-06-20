package net.blockomorph.mixins.main.coords;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.MorphMath;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
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
		return MorphMath.distanceTo(original, x, y, z, this.x, this.y, this.z, true, 0.5);
	}

	@ModifyReturnValue(method = "distToLowCornerSqr", at = @At(value = "RETURN"))
	public double sqrCorner(double original, double x, double y, double z) {
		return MorphMath.distanceTo(original, x, y, z, this.x, this.y, this.z, true, 0);
	}
}
