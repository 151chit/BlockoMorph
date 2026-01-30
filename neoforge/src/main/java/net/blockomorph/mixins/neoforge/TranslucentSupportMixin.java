package net.blockomorph.mixins.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class TranslucentSupportMixin {

	@Inject(method = "lambda$addMainPass$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1))
	public void renderMorphedPlayersTranslucent(FogParameters p_363661_, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller, Matrix4f p_362420_, Matrix4f p_361272_, ResourceHandle resourcehandle2, ResourceHandle resourcehandle3, Frustum p_366590_, boolean p_363964_, ResourceHandle resourcehandle1, ResourceHandle resourcehandle, CallbackInfo ci, @Local PoseStack stack) {
		LevelRendererAccessor.of(this).prepareTranslucentPlayersForRender$blockomorph(stack, deltaTracker, camera, profilerFiller);
	}
}
