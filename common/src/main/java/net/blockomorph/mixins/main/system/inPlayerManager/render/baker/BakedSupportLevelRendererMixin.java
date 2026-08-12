package net.blockomorph.mixins.main.system.inPlayerManager.render.baker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.textures.GpuSampler;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
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
	private void captureFrustum(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Matrix4f matrix4f, GpuBufferSlice gpuBufferSlice, boolean bl, LevelRenderState levelRenderState, DeltaTracker deltaTracker, ProfilerFiller profilerFiller, CallbackInfo ci) {
		this.thisFrameFrustum = frustum;
	}

	@WrapOperation(method = {"method_62214", "lambda$addMainPass$1"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"))
	private void render(ChunkSectionsToRender instance, ChunkSectionLayerGroup chunkSectionLayerGroup, GpuSampler gpuSampler, Operation<Void> original) {
		original.call(instance, chunkSectionLayerGroup, gpuSampler);
		if (chunkSectionLayerGroup == ChunkSectionLayerGroup.TRIPWIRE) return; //put strings into translucent
		this.asyncBakersManager.drawOnGpu(this.level, PlayerSectionLayerGroup.byChunkType(chunkSectionLayerGroup), this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), this.thisFrameFrustum);
	}

	@Inject(method = "cullTerrain", at = @At("HEAD"))
	private void setCamera(Camera camera, Frustum frustum, boolean spectator, CallbackInfo ci) {
		this.asyncBakersManager.setCameraPos(camera.position());
	}
}
