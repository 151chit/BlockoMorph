package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin { 
   @ModifyVariable(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
   private EntityHitResult onPick(EntityHitResult hit) {
     if (hit != null && hit.getEntity() instanceof PlayerAccessor pl && pl.isActive()) {
     	if (MorphUtils.hitEntity == null) {
     		return null;
     	}
     }
     return hit;
   }

   @Inject(method = "filterHitResult", at = @At("HEAD"), cancellable = true)
   private static void filter(HitResult hit, Vec3 end, double blockreach, CallbackInfoReturnable<HitResult> cir) {
     if (hit instanceof EntityHitResult h && h.getEntity() instanceof PlayerAccessor pl && pl.isActive()) {
     	Minecraft mc = Minecraft.getInstance();
     	if (MorphUtils.hitEntity != null) {
     		Vec3 vec31 = MorphUtils.hit.getLocation();
            Direction direction = Direction.getNearest(vec31.x - end.x, vec31.y - end.y, vec31.z - end.z);
            cir.setReturnValue(BlockHitResult.miss(vec31, direction, BlockPos.containing(vec31)));
     	}
     }
   }

}
