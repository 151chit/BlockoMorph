package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.blockomorph.utils.LevelRendererAccessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin implements LevelRendererAccessor {

   @Invoker("renderShape")
   private static void renderShape(PoseStack p_109783_, VertexConsumer p_109784_, VoxelShape p_109785_, double p_109786_, double p_109787_, double p_109788_, float p_109789_, float p_109790_, float p_109791_, float p_109792_) {
   }

   public void renderBlockHitbox(PoseStack p_109783_, VertexConsumer p_109784_, VoxelShape p_109785_, double p_109786_, double p_109787_, double p_109788_, float p_109789_, float p_109790_, float p_109791_, float p_109792_) {
   	  renderShape(p_109783_, p_109784_, p_109785_, p_109786_, p_109787_, p_109788_, p_109789_, p_109790_, p_109791_, p_109792_);
   }
}
