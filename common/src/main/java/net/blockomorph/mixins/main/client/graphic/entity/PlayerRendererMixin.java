package net.blockomorph.mixins.main.client.graphic.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.MorphedPlayerRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private final Consumer<Float> SHADOW = (value) -> this.shadowRadius = value;
	private final MorphedPlayerRenderer RENDERER = new MorphedPlayerRenderer();

	public PlayerRendererMixin() {
		super(null, null, 0);
	}

	@Inject(method = {"render"}, at = @At("HEAD"), cancellable = true)
	public void render(AbstractClientPlayer player, float anim, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, CallbackInfo info) {
		this.shadowRadius = 0.5f;
		if (RENDERER.render(player, anim, partialticks, posestack, buffer, light, SHADOW))
			info.cancel();
	}

}
