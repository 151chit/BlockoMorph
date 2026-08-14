package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin {

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V", ordinal = 0, at = @At(value = "HEAD"))
	public AABB getAABB(AABB orig) {
		return MorphNormalizer.tryNormalizeAabb(orig);
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V", ordinal = 0, at = @At(value = "HEAD"))
	public AABB getAABBLite(AABB orig) {
		return MorphNormalizer.tryNormalizeAabb(orig);
	}
}
