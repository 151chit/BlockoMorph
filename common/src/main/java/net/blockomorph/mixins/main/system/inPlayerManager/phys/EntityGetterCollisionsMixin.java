package net.blockomorph.mixins.main.system.inPlayerManager.phys;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.phys.MorphedCollisionGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EntityGetter.class)
public interface EntityGetterCollisionsMixin {

	@ModifyReturnValue(method = "getEntityCollisions", at = @At("RETURN"))
	private List<VoxelShape> addCustom(List<VoxelShape> original, Entity source, AABB testArea) {
		return MorphedCollisionGetter.appendAsImmutable(original, (EntityGetter) this, source, testArea);
	}
}
