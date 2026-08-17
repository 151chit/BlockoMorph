package net.blockomorph.mixins.main.system.inPlayerManager.phys.hitbox;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@FastInject(method = "getDimensions", at = @At("HEAD"))
	public Object getDimensions(Pose pose) {
		if (this instanceof PlayerAccessor acc && acc.isBlockomorphActive()) {
			return acc.getManager().getHitBoxCalculator().getDimensions();
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	@FastInject(method = "getLocalBoundsForPose", at = @At("HEAD"))
	private Object getRealPos(Pose pose) {
		if (this instanceof PlayerAccessor pl && pl.isBlockomorphFullActive()) {
			return pl.player().getDimensions(pose).makeBoundingBox(pl.player().position());
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}