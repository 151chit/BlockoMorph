package net.blockomorph.mixins.main.client.graphic.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.RenderStateAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
	private static void renderHitbox(PoseStack posestack, VertexConsumer vertex, Entity player, float f, float g, float h, float i, CallbackInfo info) {
		if (player instanceof PlayerAccessor pl) {
			if (pl.isFullActive()) info.cancel();
		}
	}

	@Inject(
			method = {"renderFlame"},
			at = {@At("HEAD")},
			cancellable = true
	)
	private void renderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, EntityRenderState entityRenderState, Quaternionf quaternionf, CallbackInfo ci) {
		if (entityRenderState instanceof RenderStateAccessor r) {
			if (r.getPl().isActive()) ci.cancel();
		}
	}
}