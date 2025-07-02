package net.blockomorph.mixins.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.ChairController;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.EntityAccessor;
import net.blockomorph.utils.tnt.TntHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(value = Entity.class, priority = 1020)
public abstract class EntityMixin implements EntityAccessor {
	@Shadow public abstract boolean hasPassenger(Entity p_20364_);
	@Shadow public abstract Level level();
	@Shadow @Final protected SynchedEntityData entityData;
	@Shadow public abstract boolean startRiding(Entity p_19966_, boolean p_19967_);

	@Shadow private Level level;
	@Unique private Vec3 fromMorphedPos;
	@Unique private static final EntityDataAccessor<CompoundTag> CHAIR_DATA = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.COMPOUND_TAG);
	private final ChairController CHAIR_CONTROLLER = new ChairController((Entity)(Object) this, CHAIR_DATA);

	public Vec3 getMorphedPos() {
		return this.fromMorphedPos;
	}

	@Nullable
	public Vec3 getSyncedPos() {
		CompoundTag tg = this.entityData.get(CHAIR_DATA);
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

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	public void init(EntityType<?> type, Level lv, CallbackInfo ci) {
		CHAIR_CONTROLLER.setEntityData(this.entityData);
	}

	@ModifyVariable(method = "<init>", at = @At("STORE"))
	public SynchedEntityData.Builder init(SynchedEntityData.Builder data) {
		data.define(CHAIR_DATA, new CompoundTag());
		return data;
	}

	@Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
	public void checkAccess(CallbackInfoReturnable<Boolean> cir) {
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

	@WrapOperation(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
	public boolean brakePlayerRidingLock(EntityType<?> instance, Operation<Boolean> original) {
		if (instance == EntityType.PLAYER) return true;
		return original.call(instance);
	}

	@Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
	public void rejectInvalidMovement(Vec3 vec3, CallbackInfo ci) {
		if ((Object)this instanceof Projectile) {
			if (vec3.x > 29_999_995 || vec3.z > 29_999_995 || vec3.z < - 29_999_995)
				ci.cancel();
		}
	}

	@Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
	public void onDataReceived(EntityDataAccessor<?> data, CallbackInfo ci) {
		CHAIR_CONTROLLER.onDataReceived(data);
		if (this instanceof PlayerAccessor acc) {
			TntHandler TNT = acc.getTntHandler();
			if (TNT != null) TNT.onClientUpdater(data);
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
}
