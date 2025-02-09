package net.blockomorph.utils;

import java.util.function.Function;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

public interface DimAccessor {
   void setListener(Function<Vec3, AABB> func);
}
