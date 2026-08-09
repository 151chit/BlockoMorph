package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Entity.class)
public abstract class EntityMixin {
	@Shadow private EntityInLevelCallback levelCallback;
	@Unique private int normalizeAttempts;

	@FastInject(method = "setPosRaw", at = @At(value = "HEAD"))
	private boolean normalize(double x, double y, double z) {
		if (this.levelCallback != EntityInLevelCallback.NULL) {
			if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
				this.normalizeAttempts++;
				if (this.normalizeAttempts > 1000) {
					MorphUtils.LOGGER.error("Attempt to forPoints entity when playerOwner unloaded?! {}", this);
					this.normalizeAttempts = 0;
					return true;
				} MorphNormalizer.normalizeEntityPos((Entity) (Object)this, x, y, z);
				return false;
			}
		}
		this.normalizeAttempts = 0;
		return true;
	}
}
