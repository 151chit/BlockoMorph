package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HitResult.class)
public class HitResultMixin {

    @Shadow @Final protected Vec3 location;

    @Inject(method = "distanceTo", at = @At("HEAD"), cancellable = true)
    public void getDist(Entity ent, CallbackInfoReturnable<Double> cir) {
        MorphUtils.distanceTo(this.location, ent.position(), true, 0, cir::setReturnValue);
    }
}
