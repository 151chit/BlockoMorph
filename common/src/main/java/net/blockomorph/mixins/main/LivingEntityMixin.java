package net.blockomorph.mixins.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
		if (this instanceof PlayerAccessor pl && !Config.get().canBeSeenByMobs.getValue() && pl.isActive()) {
			cir.setReturnValue(false);
		}
	}

	@WrapOperation(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"))
	private HitResult.Type removeSelf(BlockHitResult instance, Operation<HitResult.Type> original) {
		if (instance instanceof MorphedPlayerHitResult || InPlayerBlockPos.isMorphedPlayerX(instance.getBlockPos().getX())) {
			if (Config.get().canBeSeenByMobs.getValue()) return HitResult.Type.MISS;
		}
		return original.call(instance);
	}

	@Inject(method = "push", at = @At("HEAD"), cancellable = true)
	public void rejectPush(Entity entity, CallbackInfo ci) {
		if (this instanceof PlayerAccessor pl && pl.isActive() || entity instanceof PlayerAccessor pla && pla.isActive())
			ci.cancel();
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (this instanceof PlayerAccessor acc && acc.isActive()) {
			cir.setReturnValue(acc.getHitBoxHandler().calculateDimensions());
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
		var blocks = PlayersMultiSectionStorage.getBlockOnPos(this, this.level(), this.position());
		if (!blocks.isEmpty()) {
			return blocks.iterator().next().getBlockState().getFluidState();
		}
		return original;
	}
}
