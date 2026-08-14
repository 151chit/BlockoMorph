package net.blockomorph.mixins.main.suppressVanilla.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class ShadowsMixin {//isBlockomorphActive();

	@FastInject(method = "renderShadow", at = @At("HEAD"))
	private static boolean suppress(PoseStack poseStack, MultiBufferSource multiBufferSource, EntityRenderState state, float f, LevelReader levelReader, float g) {
		return !(state instanceof MorphedPlayerRenderState.Holder holder) || holder.getState() == null;
	}
}
