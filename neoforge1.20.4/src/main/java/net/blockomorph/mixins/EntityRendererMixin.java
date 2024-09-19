package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererMixin {

   @Inject(
      method = {"renderHitbox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderHitbox(PoseStack posestack, VertexConsumer vertex, Entity player, float delta, CallbackInfo info) {
   	  if (player instanceof PlayerAccessor pl) {
   	  	if (pl.isActive()) info.cancel();
   	  }
   }

   @Inject(
      method = {"renderFlame"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderFire(PoseStack posestack, MultiBufferSource buffer, Entity player, Quaternionf quat, CallbackInfo info) {
   	  if (player instanceof PlayerAccessor pl) {
   	  	if (pl.isActive()) info.cancel();
   	  }
   }
}
