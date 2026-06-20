package net.blockomorph.mixins.main;

import net.blockomorph.utils.MorphMath;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class SectionHandlerMixin {

	@Shadow private EntityInLevelCallback levelCallback;

	@Shadow private Vec3 position;

	@Inject(method = "setPosRaw", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
	private void setPosFor(double x, double y, double z, CallbackInfo ci) {
		if (this.levelCallback != EntityInLevelCallback.NULL && this instanceof PlayerAccessor pl) {
			var manager = pl.getSectionHandler();
			if (manager != null) {
				manager.onMove();
				if (MorphMath.isBlockPosChanged(this.position, x, y, z, pl.minPos(), pl.maxPos(), pl.minPos(), pl.maxPos())) {
					manager.getPlayerPerBlock().onMove(new Vec3(x, y, z));
				}
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
