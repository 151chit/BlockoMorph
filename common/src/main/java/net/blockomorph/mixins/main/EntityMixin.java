package net.blockomorph.mixins.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.ChairController;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.EntityAccessor;
import net.blockomorph.utils.accessors.ForceLevelChanger;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = Entity.class, priority = 1020)
public abstract class EntityMixin implements EntityAccessor, ForceLevelChanger {
	@Shadow
	public abstract boolean hasPassenger(Entity p_20364_);

	@Shadow
	public abstract Level level();

	@Shadow
	public abstract boolean startRiding(Entity p_19966_, boolean p_19967_);

	@Shadow private Level level;

	@Shadow protected abstract int getPermissionLevel();

	@Unique private Vec3 fromMorphedPos;
	private final ChairController CHAIR_CONTROLLER = new ChairController((Entity) (Object) this);

	public Vec3 getMorphedPos() {
		return this.fromMorphedPos;
	}

	@Nullable
	public Vec3 getSyncedPos() {
		CompoundTag tg = CHAIR_CONTROLLER.getTagData();
		if (!tg.isEmpty()) {
			Vec3 vec = new Vec3(tg.getDouble("x"), tg.getDouble("y"), tg.getDouble("z"));
			return InPlayerBlockPos.checkOnReal(vec);
		}
		return null;
	}

	@Inject(method = "distanceToSqr(DDD)D", at = @At(value = "RETURN"), cancellable = true)
	public void getRealCoordsSqr(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
		MorphUtils.distanceTo(new Vec3(x, y, z), ((Entity) (Object) this).position(), true, 0, cir::setReturnValue);
	}

	@Inject(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"), cancellable = true)
	public void getRealCoordsVec3(Vec3 pos, CallbackInfoReturnable<Double> cir) {
		MorphUtils.distanceTo(pos, ((Entity) (Object) this).position(), true, 0, cir::setReturnValue);
	}

	@Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
	public void checkAccess(CallbackInfoReturnable<Boolean> cir) {
		if (Config.get().hitReaction.getValue().hand) {
			return;
		}
		if (this instanceof PlayerAccessor pl && pl.isActive())
			cir.setReturnValue(false);
	}

	@Unique
	private Vec3 tempPos;

	@Inject(method = "setPosRaw", at = @At(value = "HEAD"))
	public void redirect(double x, double y, double z, CallbackInfo ci) {
		Vec3 vec = new Vec3(x, y, z);
		this.tempPos = InPlayerBlockPos.checkOnReal(vec);
		if (!this.tempPos.equals(vec))
			this.fromMorphedPos = vec;
	}

	@Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
	public void correctPos(Entity passanger, Entity.MoveFunction setPosition, CallbackInfo ci) {
		if (this.hasPassenger(passanger)) {
			Vec3 vec3;
			if (passanger instanceof EntityAccessor acc && (vec3 = acc.getSyncedPos()) != null) {
				setPosition.accept(passanger, vec3.x, vec3.y, vec3.z);
				ci.cancel();
			} else if (passanger instanceof LivingEntity livingEntity) {
				Optional<BlockPos> pos = livingEntity.getSleepingPos();
				pos.ifPresent((blockpos) -> {
					if (InPlayerBlockPos.isMorphedPlayerX(blockpos.getX())) {
						Vec3 bedPos = InPlayerBlockPos.checkOnReal(Vec3.atCenterOf(blockpos));
						setPosition.accept(passanger, bedPos.x, bedPos.y + 0.0625, bedPos.z);
						ci.cancel();
					}
				});
			}
		}
	}

	@Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
	public void rejectInvalidMovement(Vec3 vec3, CallbackInfo ci) {
		if ((Object) this instanceof Projectile) {
			if (vec3.x > 29_999_995 || vec3.z > 29_999_995 || vec3.z < -29_999_995)
				ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void tick(CallbackInfo ci) {
		CHAIR_CONTROLLER.tick();
	}

	@Inject(method = "stopRiding", at = @At(value = "HEAD"))
	public void stopRiding(CallbackInfo ci) {
		CHAIR_CONTROLLER.onStopRiding();
	}

	@Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPose(Lnet/minecraft/world/entity/Pose;)V"), cancellable = true)
	public void startRidingCheck(Entity entity, boolean p_19967_, CallbackInfoReturnable<Boolean> cir) {
		Entity ent = CHAIR_CONTROLLER.replaceEntityOnMorphedPlayer(entity);
		if (ent != entity) {
			cir.setReturnValue(true);
			this.startRiding(ent, true);
		}
	}

	@WrapOperation(method = "isInWall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;width()F"))
	private float changeHitbox(EntityDimensions instance, Operation<Float> original) {
		if (this instanceof PlayerAccessor pl && pl.isActive()) {
			AABB playerBox = pl.player().getBoundingBox();
			return (float) Math.min(playerBox.getXsize(), playerBox.getZsize());
		}
		return original.call(instance);
	}

	@ModifyVariable(method = "setPosRaw", at = @At(value = "HEAD"), ordinal = 0)
	public double getX(double value) {
		return tempPos.x;
	}

	@ModifyVariable(method = "setPosRaw", at = @At(value = "HEAD"), ordinal = 1)
	public double getY(double value) {
		return tempPos.y;
	}

	@ModifyVariable(method = "setPosRaw", at = @At(value = "HEAD"), ordinal = 2)
	public double getZ(double value) {
		return tempPos.z;
	}


	@Unique
	private Vec3 tpPos;

	@Inject(method = "absMoveTo(DDD)V", at = @At(value = "HEAD"))
	public void redirectTp(double x, double y, double z, CallbackInfo ci) {
		this.tpPos = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "absMoveTo(DDD)V", at = @At(value = "HEAD"), ordinal = 0)
	public double getXtp(double value) {
		return tpPos.x;
	}

	@ModifyVariable(method = "absMoveTo(DDD)V", at = @At(value = "HEAD"), ordinal = 1)
	public double getYtp(double value) {
		return tpPos.y;
	}

	@ModifyVariable(method = "absMoveTo(DDD)V", at = @At(value = "HEAD"), ordinal = 2)
	public double getZtp(double value) {
		return tpPos.z;
	}

	public void forceLevelChange(Level lv) {
		this.level = lv;
	}

	@Override
	public int getPermissionsLevel$blockomorph() {
		return this.getPermissionLevel();
	}
}
