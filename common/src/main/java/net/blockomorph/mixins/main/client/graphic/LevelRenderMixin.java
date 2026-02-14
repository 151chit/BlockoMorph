package net.blockomorph.mixins.main.client.graphic;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.AvatarRenderStateAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.render.MorphedPlayerRenderState;
import net.blockomorph.utils.render.MorphedPlayerRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin implements LevelRendererAccessor {
	@Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

	@Shadow @Final private SubmitNodeStorage submitNodeStorage;

	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks() {
		return this.destructionProgress;
	}

	@Unique
	private final MorphedPlayerRenderer CUSTOM_RENDERER = new MorphedPlayerRenderer();

	@Override
	public void prepareTranslucentPlayersForSubmit$blockomorph(LevelRenderState levelRenderState) {
		Profiler.get().popPush("morphedPlayerTranslucentBlocks");
		Vec3 vec3 = levelRenderState.cameraRenderState.pos;
		double x = vec3.x();
		double y = vec3.y();
		double z = vec3.z();
		PoseStack poseStack = new PoseStack();
		levelRenderState.entityRenderStates.forEach(entityRenderState -> {
			if (entityRenderState instanceof AvatarRenderStateAccessor acc && acc.getMorphedRenderStateStorage().get() != null) {
				MorphedPlayerRenderState state = acc.getMorphedRenderStateStorage().get();
				if (state.morphedState instanceof MorphedPlayerRenderState.BlockMorphedState morphedState) {
					poseStack.pushPose();
					poseStack.translate(entityRenderState.x - x, entityRenderState.y - y, entityRenderState.z - z);
					if (!MorphUtils.ONE_PHASE_PLAYER_RENDER)
						CUSTOM_RENDERER.submitTranslucentBlocks(morphedState, poseStack, this.submitNodeStorage);
					CUSTOM_RENDERER.submitFrame(morphedState, poseStack, this.submitNodeStorage);
					poseStack.popPose();
				}
			}
		});
		this.featureRenderDispatcher.renderAllFeatures();
	}

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	public void rejectChunkUpdateOnPlayer(int chunkX, int sectionY, int chunkZ, boolean yes, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphPlayerChunk(new ChunkPos(chunkX, chunkZ)))
			ci.cancel();
	}
}
