package net.blockomorph.mixins.main;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class GameEventSupportMixin {

	@Shadow private EntityInLevelCallback levelCallback;

	@Inject(method = "setPosRaw", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
	private void setPosFor(double d, double e, double f, CallbackInfo ci) {
		if (this.levelCallback != EntityInLevelCallback.NULL && this instanceof PlayerAccessor pl && pl.player().level() instanceof ServerLevelAccessor) {
			var manager = pl.getListenersStorage();
			if (manager != null) {
				manager.onMove();
			}
		}
	}

	@Inject(method = "setBoundingBox", at = @At("TAIL"))
	private void onHitBoxChange(AABB aABB, CallbackInfo ci) {
		if (this.levelCallback != EntityInLevelCallback.NULL && this instanceof PlayerAccessor pl && pl.player().level() instanceof ServerLevelAccessor) {
			var manager = pl.getListenersStorage();
			if (manager != null) {
				manager.onMove();
			}
		}
	}
}
