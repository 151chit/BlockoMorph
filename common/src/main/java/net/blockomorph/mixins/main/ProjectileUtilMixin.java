package net.blockomorph.mixins.main;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.PlayerHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

	@ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerFloat(Predicate<Entity> original) {
		if (MorphUtils.needModedHit(clazz -> {
			return clazz != ProjectileUtil.class;
		})) return original.and(PlayerHitResult.NOT_MORPHED_PLAYER);
		return original;
	}

	@ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerDouble(Predicate<Entity> original) {
		if (MorphUtils.needModedHit(clazz -> {
			return clazz != ProjectileUtil.class;
		})) return original.and(PlayerHitResult.NOT_MORPHED_PLAYER);
		return original;
	}

	@ModifyVariable(method = "getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;Z)Ljava/util/Collection;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerMany(Predicate<Entity> original) {
		if (MorphUtils.needModedHit(clazz -> {
			return clazz != ProjectileUtil.class;
		})) return original.and(PlayerHitResult.NOT_MORPHED_PLAYER);
		return original;
	}

	@ModifyVariable(method = "getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;", at = @At(value = "STORE"), ordinal = 2)
	private static Vec3 clampVec33(Vec3 value) {
		return InPlayerBlockPos.checkOnReal(value);
	}
}
