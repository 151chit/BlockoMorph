package net.blockomorph.mixins.main.client.graphic.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.MorphedPlayerRenderer;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.RenderStateAccessor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> extends EntityRenderer<T, S> {
	private final Consumer<Float> SHADOW = (value) -> this.shadowRadius = value;
	private final MorphedPlayerRenderer RENDERER = new MorphedPlayerRenderer();

	public LivingEntityRendererMixin() {
		super(null);
	}

	@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
	public void render(S livingEntityRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
		if (livingEntityRenderState instanceof RenderStateAccessor acc) {
			this.shadowRadius = 0.5f;
			if (RENDERER.render(acc.getPlayer(), acc.tick().floatValue(), poseStack, multiBufferSource, i, SHADOW))
				ci.cancel();
			if (MorphUtils.ONE_PHASE_PLAYER_RENDER && acc.getPlayer() instanceof PlayerAccessor pl && pl.isFullActive()) {
				RENDERER.renderTranslucentBlocks(pl, poseStack, multiBufferSource);
			}
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	public void extractRenderState(T abstractClientPlayer, S playerRenderState, float f, CallbackInfo ci) {
		if (playerRenderState instanceof RenderStateAccessor r && abstractClientPlayer instanceof AbstractClientPlayer player) {
			r.loadPlayer(player);
			r.tick().setValue(f);
		}
	}

}
