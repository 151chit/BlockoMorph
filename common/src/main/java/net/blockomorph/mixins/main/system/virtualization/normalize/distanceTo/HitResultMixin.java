package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.math.MorphDistanceTo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HitResult.class)
public class HitResultMixin {
	@Shadow @Final protected Vec3 location;

	@ModifyReturnValue(method = "distanceTo", at = @At("RETURN"))
	public double getDist(double original, Entity entity) {
		return MorphDistanceTo.forPoints(original, this.location, entity.position(), true, 0);
	}
}
