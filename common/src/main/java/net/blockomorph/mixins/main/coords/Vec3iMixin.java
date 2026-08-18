package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vec3i.class)
public class Vec3iMixin {
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int z;

	@Inject(method = "distToCenterSqr(DDD)D", at = @At(value = "RETURN"), cancellable = true)
	public void sqr(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
		MorphUtils.distanceTo(new Vec3(x, y, z), new Vec3(this.x, this.y, this.z), true, 0.5D, cir::setReturnValue);
	}

	@Inject(method = "distToLowCornerSqr", at = @At(value = "RETURN"), cancellable = true)
	public void sqrCorner(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
		MorphUtils.distanceTo(new Vec3(x, y, z), new Vec3(this.x, this.y, this.z), true, 0, cir::setReturnValue);
	}
}
