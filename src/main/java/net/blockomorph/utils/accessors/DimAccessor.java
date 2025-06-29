package net.blockomorph.utils.accessors;

import java.util.function.Function;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

public interface DimAccessor {
   void setListener(Function<Vec3, AABB> func);

   static EntityDimensions dynamic(Function<Vec3, AABB> func, float eye) {
      EntityDimensions dummy = EntityDimensions.fixed(0, 0).withEyeHeight(eye);
      ((DimAccessor)(Object)dummy).setListener(func);
      return dummy;
   }
}
