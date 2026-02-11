package net.blockomorph.mixins.compat.lithium;

import com.google.common.collect.Iterables;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions", remap = false)
public class EntityCollisionsMixin {

	@Inject(method = "appendEntityCollisions", at = @At("TAIL"))
	private static void addEntityCollisions(List<VoxelShape> entityCollisions, Level world, Entity entity, AABB box, CallbackInfo ci) {
		MorphUtils.fillListWithPlayerCollisions(world, entity, box, entityCollisions);
	}

	@Inject(method = "getEntityWorldBorderCollisionIterable", at = @At("RETURN"), cancellable = true)
	private static void getEntityCollisionsIterator(EntityGetter view, @Nullable Entity entity, AABB box, boolean includeWorldBorder, CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		Iterable<VoxelShape> shapeIterator = cir.getReturnValue();
		if (shapeIterator != null) {
			List<VoxelShape> playerShapes = new ArrayList<>();
			MorphUtils.fillListWithPlayerCollisions(view, entity, box, playerShapes);
			cir.setReturnValue(Iterables.concat(shapeIterator, playerShapes));
		}
	}
}
