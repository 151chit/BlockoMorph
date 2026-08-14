package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import net.blockomorph.core.misc.chairController.EntityChairController;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements EntityChairController.ChairedEntity {
	@Shadow @Final public WalkAnimationState walkAnimation;
	@Unique private final EntityChairController controller = new EntityChairController((LivingEntity) (Object)this);

	@Override
	public EntityChairController getController() {
		return this.controller;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void ride(CallbackInfo ci) {
		this.controller.rideTick(false);
	}

	@FastInject(method = "travel", at = @At("HEAD"))
	private boolean suppressMove(Vec3 input) {
		return !this.controller.isActive();
	}

	@FastInject(method = "calculateEntityAnimation", at = @At("HEAD"))
	private boolean reject(boolean useY) {
		boolean active = this.controller.isActive();
		if (active) {
			this.walkAnimation.stop();
		}
		return !active;
	}
}
