package net.blockomorph.mixins.main.system.inPlayerManager.render.state;

import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerRenderState.class)
public class HolderAvatarRenderStateMixin implements MorphedPlayerRenderState.Holder {
	@Unique private MorphedPlayerRenderState state;

	@Override
	public MorphedPlayerRenderState getState() {
		return this.state;
	}

	@Override
	public void setRenderState(MorphedPlayerRenderState state) {
		this.state = state;
	}
}
