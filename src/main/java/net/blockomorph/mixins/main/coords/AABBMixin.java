package net.blockomorph.mixins.main.coords;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AABB.class)
public abstract class AABBMixin {
	@Shadow public abstract Vec3 getCenter();

	@Inject(method = "distanceToSqr", at = @At("HEAD"), cancellable = true)
	public void distance(Vec3 vec3, CallbackInfoReturnable<Double> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(vec3.x) || InPlayerBlockPos.isMorphedPlayerX(this.getCenter().x)) {
			AABB aabb = InPlayerBlockPos.checkOnReal((AABB) (Object)this);
			Vec3 vec = InPlayerBlockPos.checkOnReal(vec3);
			if (!InPlayerBlockPos.isMorphedPlayerX(vec.x) && !InPlayerBlockPos.isMorphedPlayerX(aabb.getCenter().x)) {
				cir.setReturnValue(aabb.distanceToSqr(vec));
			}
		}
	}
}