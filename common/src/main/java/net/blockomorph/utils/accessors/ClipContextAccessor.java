package net.blockomorph.utils.accessors;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;

public interface ClipContextAccessor {

	CollisionContext getContext();

	ClipContext.Block getMode();

	ClipContext.Fluid getFluidMode();

	ClipContext normalize();

	static ClipContextAccessor of(ClipContext ctx) {
		return (ClipContextAccessor) ctx;
	}
}
