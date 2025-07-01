package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Vec3.class, priority = 1020)
public class Vec3Mixin {

    @Inject(method = "distanceTo", at = @At(value = "RETURN"), cancellable = true)
    public void getRealCoords(Vec3 vec3, CallbackInfoReturnable<Double> cir) {
        this.normalizeCoords(vec3, false, cir);
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At(value = "RETURN"), cancellable = true)
    public void getRealCoordsSqr(Vec3 vec3, CallbackInfoReturnable<Double> cir) {
        this.normalizeCoords(vec3, true, cir);
    }

    @Inject(method = "distanceToSqr(DDD)D", at = @At(value = "RETURN"), cancellable = true)
    public void getRealCoordsSqr(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        this.normalizeCoords(new Vec3(x, y, z), true, cir);
    }

    private void normalizeCoords(Vec3 vec3, boolean sqr, CallbackInfoReturnable<Double> cir) {
        MorphUtils.distanceTo(vec3, (Vec3) (Object) this, sqr, 0, cir::setReturnValue);
    }
}
