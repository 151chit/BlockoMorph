package net.blockomorph.mixins.main.suppressVanilla.render;

import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class HitboxesMixin {

	@FastInject(method = "extractHitboxes(Lnet/minecraft/world/entity/Entity;FZ)Lnet/minecraft/client/renderer/entity/state/HitboxesRenderState;", at = @At("HEAD"))
	private Object renderHitboxes(Entity entity, float partialTicks, boolean isServerEntity) {
		if (EarlyLoadingPlatformService.INSTANCE.isRunningInIde() || PlayersStorage.NOT_MORPHED_PLAYER.test(entity))
			return FastInject.CONTINUE_EXECUTION;
		return null;
	}
}
