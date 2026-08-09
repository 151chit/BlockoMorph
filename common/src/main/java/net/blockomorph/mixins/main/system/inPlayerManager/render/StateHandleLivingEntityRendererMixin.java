package net.blockomorph.mixins.main.system.inPlayerManager.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.render.SubmitNodeCollectorWrapper;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class StateHandleLivingEntityRendererMixin {

	@Inject(at = @At("HEAD"), method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V")
	private void extractState(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		if (state instanceof MorphedPlayerRenderState.Holder holder && entity instanceof PlayerAccessor pl && pl.getManager() instanceof ClientInPlayerManager mn) {
			var stateExtractor = mn.getRenderer().getStateExtractor();
			var renderState = stateExtractor.createRenderState();
			stateExtractor.extractRenderState(renderState, partialTicks);
			holder.setRenderState(renderState);
		}
	}

	@FastInject(at = @At("HEAD"), method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V")
	private boolean render(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state instanceof MorphedPlayerRenderState.Holder holder) {
			var renderState = holder.getState();
			if (renderState != null) {
				renderState.renderer().submit(poseStack, renderState, SubmitNodeCollectorWrapper.INSTANCE.setAdditionalData(camera));
				return false;
			}
		}
		return true;
	}
}
