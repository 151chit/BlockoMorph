package net.blockomorph.mixins.main.suppressVanilla;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileHitResultMixin {

	@ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerFloat(Predicate<Entity> original, @Local(argsOnly = true) Entity source) {
		if (MorphUtils.canUseMorphLogicForProjectile(source)) return original.and(PlayersStorage.NOT_MORPHED_PLAYER);
		return original;
	}

	@ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerDouble(Predicate<Entity> original, @Local(argsOnly = true) Entity source) {
		if (MorphUtils.canUseMorphLogicForProjectile(source)) return original.and(PlayersStorage.NOT_MORPHED_PLAYER);
		return original;
	}

	@ModifyVariable(method = "getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;Z)Ljava/util/Collection;", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayers(Predicate<Entity> original, @Local(argsOnly = true) Entity source) {
		if (MorphUtils.canUseMorphLogicForProjectile(source)) return original.and(PlayersStorage.NOT_MORPHED_PLAYER);
		return original;
	}
}
