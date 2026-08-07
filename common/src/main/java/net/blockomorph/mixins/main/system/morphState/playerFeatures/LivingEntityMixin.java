package net.blockomorph.mixins.main.system.morphState.playerFeatures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.config.Config;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@FastInject(method = "canBeSeenByAnyone", at = @At("RETURN"))
	public byte checkSeen() {
		if (this instanceof PlayerAccessor pl && !Config.get().canBeSeenByMobs.getValue() && pl.isBlockomorphActive()) {
			return -1;
		}
		return 0;
	}

	@WrapOperation(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"))
	private HitResult.Type removeSelf(BlockHitResult instance, Operation<HitResult.Type> original) {
		if (instance instanceof MorphedPlayerHitResult || InPlayerBlockPos.isMorphedPlayerBlockX(instance.getBlockPos().getX())) {
			if (Config.get().canBeSeenByMobs.getValue()) return HitResult.Type.MISS;
		}
		return original.call(instance);
	}

	@FastInject(method = "push", at = @At("HEAD"))
	public boolean rejectPush(Entity entity) {
		return PlayersStorage.NOT_MORPHED_PLAYER.test((Entity)(Object) this) && PlayersStorage.NOT_MORPHED_PLAYER.test(entity);
	}
}