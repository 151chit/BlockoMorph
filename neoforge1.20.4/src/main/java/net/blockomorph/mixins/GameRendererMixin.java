package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin { 
   @ModifyVariable(method = "pick", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
   public EntityHitResult onPick(EntityHitResult hit) {
     if (hit != null && hit.getEntity() instanceof PlayerAccessor pl && pl.isActive()) {
     	Minecraft mc = Minecraft.getInstance();
     	if (MorphUtils.hitEntity != null) {
     		Vec3 vec = mc.getCameraEntity().getViewVector(1.0F);
     		Vec3 target = hit.getLocation();
     		mc.hitResult = BlockHitResult.miss(target, Direction.getNearest(vec.x, vec.y, vec.z), BlockPos.containing(target));
     	}
     	return null;
     }
     return hit;
   }

}
