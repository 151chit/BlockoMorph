package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 999)
public class ClientLevelMixin {

	@Inject(method = "addEntity", at = @At("HEAD"))
	private void normalize(Entity entity, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(entity.getX())) {
			MorphNormalizer.normalizeEntityPos(entity);
		}
	}
}
