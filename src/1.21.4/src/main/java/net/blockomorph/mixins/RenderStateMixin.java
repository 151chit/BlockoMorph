package net.blockomorph.mixins;

import net.blockomorph.utils.RenderStateAccessor;

import net.minecraft.client.renderer.entity.state.PlayerRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.AbstractClientPlayer;

@Mixin(PlayerRenderState.class)
public class RenderStateMixin implements RenderStateAccessor {
	public AbstractClientPlayer player;

	public void loadPlayer(AbstractClientPlayer pl) {
		this.player = pl;
	}

	public AbstractClientPlayer getPlayer() {
		return this.player;
	}
}
