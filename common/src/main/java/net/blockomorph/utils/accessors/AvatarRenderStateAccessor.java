package net.blockomorph.utils.accessors;

import net.blockomorph.utils.render.MorphedPlayerRenderState;

import java.util.concurrent.atomic.AtomicReference;

public interface AvatarRenderStateAccessor {
	AtomicReference<MorphedPlayerRenderState> getMorphedRenderStateStorage();
}
