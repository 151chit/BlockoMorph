package net.blockomorph.mixins.main.system.morphState.morphAdapt.excludeFromSearch;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(Level.class)
public class LevelMixin {

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"))
	private Predicate<Entity> eraseOwner(Predicate<Entity> original, Entity ent, AABB aabb) {
		return this.checkPredicate(aabb, original);
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V", at = @At("HEAD"))
	private <T extends Entity> Predicate<Entity> eraseOwner(Predicate<Entity> original, EntityTypeTest<Entity, T> type, AABB aabb) {
		return this.checkPredicate(aabb, original);
	}

	@Unique
	private <T> Predicate<T> checkPredicate(AABB aabb, Predicate<T> original) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(
				Mth.floor(Mth.lerp(0.5d, aabb.minX, aabb.maxX)),
				Mth.floor(Mth.lerp(0.5d, aabb.minZ, aabb.maxZ)));
		if (pl != null) {
			return original.and(e -> e != pl.player());
		}
		return original;
	}
}
