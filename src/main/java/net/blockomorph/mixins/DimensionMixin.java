package net.blockomorph.mixins;

import net.blockomorph.utils.*;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Debug;
import java.util.function.Function;
import net.minecraft.world.phys.Vec3;

@Debug(export = true)
@Mixin(EntityDimensions.class)
public class DimensionMixin implements DimAccessor {
   private Function<Vec3, AABB> func;

   @Inject(method = "makeBoundingBox(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
   public void makeBoundingBox(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
   	   if (func != null) cir.setReturnValue(this.func.apply(new Vec3(x, y, z)));
   }

   public void setListener(Function<Vec3, AABB> func) {
   	   this.func = func;
   }
}
