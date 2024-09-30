package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.border.WorldBorder;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

@Mixin(Level.class)
public abstract class EntityGetterMixin implements LevelAccessor, CollisionGetter {

   @Override
   public List<VoxelShape> getEntityCollisions(@Nullable Entity p_186451_, AABB p_186452_) {

   	  if (p_186452_.getSize() < 1.0E-7D) {
         return List.of();
      } else {
         Predicate<Entity> predicate = p_186451_ == null ? EntitySelector.CAN_BE_COLLIDED_WITH : 
         EntitySelector.NO_SPECTATORS.and(entity -> entity instanceof Player || p_186451_.canCollideWith(entity));
         
         List<Entity> list = this.getEntities(p_186451_, p_186452_.inflate(1.0E-7D), predicate);
         if (list.isEmpty()) {
            return List.of();
         } else {
            ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

            for(Entity entity : list) {
               if (entity instanceof PlayerAccessor pl) {
               	  VoxelShape shape = pl.getShape();
        
                  shape = shape.move(entity.getX() - 0.5, entity.getY(), entity.getZ() - 0.5);

                  if (pl.isActive())
               	      builder.add(shape);
               } else {
                  builder.add(Shapes.create(entity.getBoundingBox()));
               }
            }
            return builder.build();

         }
      }
   }

   @Override
   public boolean noCollision(@Nullable Entity p_45757_, AABB p_45758_) {
      for(VoxelShape voxelshape : this.getBlockCollisions(p_45757_, p_45758_)) {
         if (!voxelshape.isEmpty()) {
            return false;
         }
      }
      
      for (VoxelShape entityShape : this.getEntityCollisions(p_45757_, p_45758_)) {
         if (Shapes.joinIsNotEmpty(entityShape, Shapes.create(p_45758_), BooleanOp.AND)) {
            return false;
         }
      }

      if (p_45757_ == null) {
         return true;
      } else {
         VoxelShape voxelshape1 = this.borderCollision(p_45757_, p_45758_);
         return voxelshape1 == null || !Shapes.joinIsNotEmpty(voxelshape1, Shapes.create(p_45758_), BooleanOp.AND);
      }
   }

   @Nullable
   private VoxelShape borderCollision(Entity p_186441_, AABB p_186442_) {
      WorldBorder worldborder = this.getWorldBorder();
      return worldborder.isInsideCloseToBorder(p_186441_, p_186442_) ? worldborder.getCollisionShape() : null;
   }
}
