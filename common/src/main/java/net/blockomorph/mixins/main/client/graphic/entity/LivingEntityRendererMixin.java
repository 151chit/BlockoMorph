package net.blockomorph.mixins.main.client.graphic.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.AvatarRenderStateAccessor;
import net.blockomorph.utils.render.MorphedPlayerRenderState;
import net.blockomorph.utils.render.MorphedPlayerRenderer;
import net.blockomorph.utils.render.MorphedRenderStateExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> extends EntityRenderer<T, S> {
	private final MorphedPlayerRenderer RENDERER = new MorphedPlayerRenderer();

	public LivingEntityRendererMixin() {
		super(null);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	public void render(S livingEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
		if (livingEntityRenderState instanceof AvatarRenderStateAccessor acc) {
			MorphedPlayerRenderState state = acc.getMorphedRenderStateStorage().get();
			if (RENDERER.submitMorphedState(poseStack, state, submitNodeCollector, cameraRenderState)) {
				ci.cancel();
			}
			if (MorphUtils.ONE_PHASE_PLAYER_RENDER && state.morphedState instanceof MorphedPlayerRenderState.BlockMorphedState st) {
				RENDERER.submitTranslucentBlocks(st, poseStack, submitNodeCollector);
			}
			if (state != null && state.isBlock()) {
				livingEntityRenderState.shadowRadius = 0;
				livingEntityRenderState.shadowPieces.clear();
			}
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = {@At("TAIL")})
	public void extractRenderState(T abstractClientPlayer, S playerRenderState, float deltaTick, CallbackInfo ci) {
		if (playerRenderState instanceof AvatarRenderStateAccessor avatarRenderState &&
				playerRenderState.entityType == EntityType.PLAYER && abstractClientPlayer instanceof PlayerAccessor pl) {
			MorphedPlayerRenderState state = MorphedRenderStateExtractor.extractRenderState(pl, new MorphedPlayerRenderState(), deltaTick);
			avatarRenderState.getMorphedRenderStateStorage().set(state);
			if (state.morphedState != null) {
				playerRenderState.displayFireAnimation = false;
			}
		}
	}

}
