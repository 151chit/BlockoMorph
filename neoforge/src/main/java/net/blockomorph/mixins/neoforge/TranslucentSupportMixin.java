package net.blockomorph.mixins.neoforge;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class TranslucentSupportMixin {

	@Inject(method = "lambda$addMainPass$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1))
	public void renderTranslucentPlayers(GpuBufferSlice p_418185_, LevelRenderState levelRenderState, ProfilerFiller p_362234_, Matrix4f p_362420_, ResourceHandle resourcehandle2, ResourceHandle resourcehandle3, boolean p_363964_, ResourceHandle resourcehandle1, ResourceHandle resourcehandle, CallbackInfo ci) {
		LevelRendererAccessor.of(this).prepareTranslucentPlayersForSubmit$blockomorph(levelRenderState);
	}
}
