package net.blockomorph.mixins.main.system.inPlayerManager.render.baker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class BakedSupportLevelRendererMixin implements PlayersAsyncBakersManager.Provider {
	@Shadow private @Nullable ClientLevel level;
	@Shadow @Final private Minecraft minecraft;
	@Unique private final PlayersAsyncBakersManager asyncBakersManager =
			new PlayersAsyncBakersManager(PlayersAsyncBakersManager.createPool(), true);
	@Unique private Frustum thisFrameFrustum;

	@Override
	public PlayersAsyncBakersManager getBaker() {
		return this.asyncBakersManager;
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void closeRes(CallbackInfo ci) {
		this.asyncBakersManager.close();
	}

	@Inject(method = "addMainPass", at = @At("HEAD"))
	private void captureFrustum(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, FogParameters fogParameters, boolean bl, boolean bl2, DeltaTracker deltaTracker, ProfilerFiller profilerFiller, CallbackInfo ci) {
		this.thisFrameFrustum = frustum;
	}

	@WrapOperation(method = {"method_62214", "lambda$addMainPass$2"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
	private void render(LevelRenderer instance, RenderType renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, Operation<Void> original) {
		original.call(instance, renderType, d, e, f, matrix4f, matrix4f2);
		PlayerSectionLayerGroup group = null;
		if (renderType == RenderType.solid()) {
			group = PlayerSectionLayerGroup.OPAQUE;
		} else if (renderType == RenderType.translucent()) {
			group = PlayerSectionLayerGroup.TRANSLUCENT;
		}
		if (group != null)
			this.asyncBakersManager.drawOnGpu(this.level, group, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), this.thisFrameFrustum);
	}

	@Inject(method = "setupRender", at = @At("HEAD"))
	private void setCamera(Camera camera, Frustum frustum, boolean bl, boolean bl2, CallbackInfo ci) {
		this.asyncBakersManager.setCameraPos(camera.getPosition());
	}
}
