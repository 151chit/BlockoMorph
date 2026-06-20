package net.blockomorph.mixins.main.coords;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.MorphMath;
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
	public double getDist(double original, Entity ent) {
		return MorphMath.distanceTo(original, this.location, ent.position(), true, 0);
	}
}
