package net.blockomorph.mixins.main.system.morphState.playerFeatures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {

	@FastInject(method = "isAttackable", at = @At("HEAD"))
	public byte suppressAttacks() {
		if (!Config.get().hitReaction.getValue().hand && this instanceof PlayerAccessor pl && pl.isBlockomorphActive())
			return -1;
		return 0;
	}

	@WrapOperation(method = "isInWall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;width()F"))
	private float changeHitboxInWall(EntityDimensions instance, Operation<Float> original) {
		if (this instanceof PlayerAccessor pl && pl.isBlockomorphActive()) {
			double xSize = pl.maxPos().x - pl.minPos().x;
			double zSize = pl.maxPos().z - pl.minPos().z;
			return Mth.floor(Math.min(xSize, zSize));
		}
		return original.call(instance);
	}

	@FastInject(method = "isOnFire", at = @At("HEAD"))
	private byte suppress() {
		if (this instanceof PlayerAccessor pl && pl.isBlockomorphActive())
			return -1;
		return 0;
	}
}
