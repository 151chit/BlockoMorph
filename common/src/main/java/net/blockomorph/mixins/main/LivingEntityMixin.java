package net.blockomorph.mixins.main;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

	public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
		super(p_19870_, p_19871_);
	}

	@Inject(method = "canBeSeenByAnyone", at = @At("RETURN"), cancellable = true)
	public void checkSeen(CallbackInfoReturnable<Boolean> cir) {
		boolean result = cir.getReturnValue();
		if (this instanceof PlayerAccessor pl && pl.isActive()) {
			cir.setReturnValue(false);
			return;
		}
		cir.setReturnValue(result);
	}

	@Inject(method = "push", at = @At("HEAD"), cancellable = true)
	public void rejectPush(Entity entity, CallbackInfo ci) {
		if (this instanceof PlayerAccessor pl && pl.isActive())
			ci.cancel();
	}

	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	public void rejectPushEntities(CallbackInfo ci) {
		if (this instanceof PlayerAccessor pl && pl.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "getLocalBoundsForPose", at = @At("HEAD"), cancellable = true)
	private void getRealPos(Pose pose, CallbackInfoReturnable<AABB> cir) {
		if (this instanceof PlayerAccessor pl && pl.isFullActive()) {
			cir.setReturnValue(pl.player().getDimensions(pose).makeBoundingBox(pl.player().position()));
		}
	}

	@ModifyVariable(method = "travel", at = @At("STORE"))
	private FluidState modifyState(FluidState original) {
		var entry = MorphUtils.getLiquidOnPos(this, this.position());
		if (entry != null) {
			return entry.getValue().getBlockState().getFluidState();
		}
		return original;
	}
}
