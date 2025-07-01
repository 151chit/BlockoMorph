package net.blockomorph.mixins.main.client.graphic.entity;

import net.blockomorph.utils.accessors.RenderStateAccessor;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;

import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.player.AbstractClientPlayer;

@Mixin(PlayerRenderState.class)
public abstract class RenderStateMixin implements RenderStateAccessor {
	private AbstractClientPlayer player;
	private final MutableFloat delta = new MutableFloat();

	public void loadPlayer(AbstractClientPlayer pl) {
		this.player = pl;
	}

	public AbstractClientPlayer getPlayer() {
		return this.player;
	}

	@Override
	public MutableFloat tick() {
		return this.delta;
	}
}