package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.blockomorph.utils.*;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererMixin {

   @Inject(
      method = {"renderHitbox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderHitbox(PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, float f, float g, float h, float i, CallbackInfo ci) {
   	  if (entity instanceof PlayerAccessor pl) {
   	  	if (pl.isFullActive()) ci.cancel();
   	  }
   }

   @Inject(
      method = {"renderFlame"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderFlame(PoseStack poseStack, MultiBufferSource multiBufferSource, EntityRenderState entityRenderState, Quaternionf quaternionf, CallbackInfo ci) {
      if (entityRenderState instanceof RenderStateAccessor r) {
   	    PlayerAccessor pl = (PlayerAccessor)r.getPlayer();
   	  	if (pl.isActive()) ci.cancel();
   	  }
   }
}
