package net.blockomorph.mixins.main.suppressVanilla.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class HitboxesMixin {

	@FastInject(method = "renderShadow", at = @At("HEAD"))
	private static boolean renderHitboxes(PoseStack poseStack, MultiBufferSource multiBufferSource, EntityRenderState entityRenderState, float f, float g, LevelReader levelReader, float h) {
		if (EarlyLoadingPlatformService.INSTANCE.isRunningInIde())
			return true;
		return !(entityRenderState instanceof MorphedPlayerRenderState.Holder holder) || holder.getState() == null;
	}
}
