package net.blockomorph.mixins.compat.lithium;

import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.phys.MorphedCollisionGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions")
public class EntityCollisionsMixin {

	@Inject(method = "appendEntityCollisions", at = @At("TAIL"))
	private static void addEntityCollisions(List<VoxelShape> entityCollisions, Level world, Entity entity, AABB box, CallbackInfo ci) {
		MorphedCollisionGetter.appendEntityCollisions(entityCollisions, world, entity, box);
	}

	@ModifyReturnValue(method = "getEntityWorldBorderCollisionIterable", at = @At("RETURN"))//todo
	private static Iterable<VoxelShape> addEntityCollisionsIterable(Iterable<VoxelShape> original, EntityGetter view, @Nullable Entity entity, AABB box) {
		ArrayList<VoxelShape> playerShapes = new ArrayList<>();
		MorphedCollisionGetter.appendEntityCollisions(playerShapes, view, entity, box);
		return Iterables.concat(original, playerShapes);
	}
}
