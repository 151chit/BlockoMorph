package net.blockomorph.mixins.main.client.graphic.entity;

import net.blockomorph.utils.accessors.AvatarRenderStateAccessor;
import net.blockomorph.utils.render.MorphedPlayerRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(AvatarRenderState.class)
public abstract class AvatarAvatarRenderStateMixin implements AvatarRenderStateAccessor {
	@Unique private final AtomicReference<MorphedPlayerRenderState> morphedState = new AtomicReference<>();

	public AtomicReference<MorphedPlayerRenderState> getMorphedRenderStateStorage() {
		return this.morphedState;
	}
}
