package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Entity.class)
public abstract class EntityAcc implements Accessors.EntityAccessor {
	@Shadow protected abstract void unsetRemoved();
	@Shadow protected abstract void positionRider(Entity passenger, Entity.MoveFunction moveFunction);

	@Override
	public void unmarkRemoved$bm() {
		this.unsetRemoved();
	}

	@Override
	public void positionRider$bm(Entity passenger, Entity.MoveFunction function) {
		this.positionRider(passenger, function);
	}
}