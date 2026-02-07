package net.blockomorph.mixins.main.client.graphic.entity;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxesMixin {

	@Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
	private void renderHitboxes(Entity entity, float f, boolean bl, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor acc && acc.isFullActive()) ci.cancel();
	}
}
