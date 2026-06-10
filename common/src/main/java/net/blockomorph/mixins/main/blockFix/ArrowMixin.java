package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractArrow.class)
public abstract class ArrowMixin extends Entity {

	public ArrowMixin() {
		super(null, null);
	}

	@ModifyVariable(method = "onHitBlock", at = @At("STORE"))
	public Vec3 normalize(Vec3 vec, BlockHitResult hit) {
		return InPlayerBlockPos.checkOnReal(hit.getLocation()).subtract(this.getX(), this.getY(), this.getZ());
	}
}
