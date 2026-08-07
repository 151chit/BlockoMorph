package net.blockomorph.mixins.main.suppressVanilla.render;

import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxesMixin {

	@FastInject(method = "showHitboxes", at = @At("HEAD"))
	private boolean renderHitboxes(Entity entity, float partialTicks, boolean isServerEntity) {
		return PlayersStorage.NOT_MORPHED_PLAYER.test(entity);
	}
}
