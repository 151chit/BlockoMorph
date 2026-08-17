package net.blockomorph.mixins.main.system.inPlayerManager.sectionStorage;

import net.blockomorph.core.PlayerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

	@Shadow private EntityInLevelCallback levelCallback;

	@Inject(method = "setBoundingBox", at = @At("TAIL"))
	private void onMoveHitbox(AABB bb, CallbackInfo ci) {
		if (this.levelCallback != EntityInLevelCallback.NULL && this instanceof PlayerAccessor pl) {
			var manager = pl.getSectionHandler();
			if (manager != null) {
				manager.onMove();
			}
		}
	}

	@Inject(method = "setBoundingBox", at = @At("TAIL"))
	private void onHitBoxChange(AABB aABB, CallbackInfo ci) {
		if (this.levelCallback != EntityInLevelCallback.NULL && this instanceof PlayerAccessor pl) {
			var manager = pl.getSectionHandler();
			if (manager != null) {
				manager.onHitboxChange();
			}
		}
	}
}
