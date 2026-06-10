package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntitySectionStorage.class)
public class EntitySectionMixin {

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V", ordinal = 0, at = @At(value = "HEAD"))
	public AABB getAABB(AABB orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V", ordinal = 0, at = @At(value = "HEAD"))
	public AABB getAABBLite(AABB orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}
}
