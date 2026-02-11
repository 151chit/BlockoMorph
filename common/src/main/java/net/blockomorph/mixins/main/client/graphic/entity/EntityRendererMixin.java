package net.blockomorph.mixins.main.client.graphic.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererMixin {

	@Inject(
			method = {"renderHitbox"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private static void renderHitbox(PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, float f, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl) {
			if (pl.isFullActive()) ci.cancel();
		}
	}

	@Inject(
			method = {"renderFlame"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private void renderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl) {
			if (pl.isActive()) ci.cancel();
		}
	}
}