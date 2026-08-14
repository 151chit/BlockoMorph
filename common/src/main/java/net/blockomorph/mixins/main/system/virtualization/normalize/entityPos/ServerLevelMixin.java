package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerLevel.class, priority = 999)
public class ServerLevelMixin {

	@Inject(method = "addEntity", at = @At("HEAD"))
	private void normalize(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(entity.getX())) {
			MorphNormalizer.normalizeEntityPos(entity);
		}
	}
}
