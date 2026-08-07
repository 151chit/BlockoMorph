package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public class ClipContextAcc implements Accessors.ClipContextAccessor {
	@Shadow @Final private CollisionContext collisionContext;
	@Override
	public CollisionContext getCollisionCtx$bm() {
		return this.collisionContext;
	}
}
