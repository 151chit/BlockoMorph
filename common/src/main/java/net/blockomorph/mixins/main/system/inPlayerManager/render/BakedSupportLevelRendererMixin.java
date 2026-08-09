package net.blockomorph.mixins.main.system.inPlayerManager.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.textures.GpuSampler;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class BakedSupportLevelRendererMixin implements PlayersAsyncBakersManager.Provider {
	@Shadow private @Nullable ClientLevel level;
	@Shadow @Final private Minecraft minecraft;
	@Unique private final PlayersAsyncBakersManager asyncBakersManager =
			new PlayersAsyncBakersManager(PlayersAsyncBakersManager.createPool(), true);

	@Override
	public PlayersAsyncBakersManager getBaker() {
		return this.asyncBakersManager;
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void closeRes(CallbackInfo ci) {
		this.asyncBakersManager.close();
	}

	@WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"))
	private void render(ChunkSectionsToRender instance, ChunkSectionLayerGroup group, GpuSampler sampler, Operation<Void> original) {
		original.call(instance, group, sampler);
		this.asyncBakersManager.drawOnGpu(this.level, PlayerSectionLayerGroup.byChunkType(group), this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), this.minecraft.gameRenderer.getMainCamera().getCullFrustum());
	}

	@Inject(method = "cullTerrain", at = @At("HEAD"))
	private void setCamera(Camera camera, Frustum frustum, boolean spectator, CallbackInfo ci) {
		this.asyncBakersManager.setCameraPos(camera.position());
	}
}
