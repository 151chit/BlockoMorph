package net.blockomorph.mixins.main;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public class AvatarMixin {

	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensionsDefault(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (this instanceof PlayerAccessor acc && acc.isActive()) {
			cir.setReturnValue(acc.getHitBoxHandler().calculateDimensions());
		}
	}
}
