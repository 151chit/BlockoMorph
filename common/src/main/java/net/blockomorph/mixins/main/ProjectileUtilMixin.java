package net.blockomorph.mixins.main;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.hit.PlayerHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
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
}
