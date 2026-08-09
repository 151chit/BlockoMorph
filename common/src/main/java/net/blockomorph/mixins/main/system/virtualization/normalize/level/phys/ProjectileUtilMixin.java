package net.blockomorph.mixins.main.system.virtualization.normalize.level.phys;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

	@ModifyVariable(method = "getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;",
			at = @At(value = "STORE"), ordinal = 2)
	private static Vec3 clampVec33(Vec3 to) {
		return MorphNormalizer.normalize(to);
	}
}
