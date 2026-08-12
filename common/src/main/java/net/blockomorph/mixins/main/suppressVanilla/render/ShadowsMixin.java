package net.blockomorph.mixins.main.suppressVanilla.render;

import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class ShadowsMixin<S extends EntityRenderState> {//isBlockomorphActive();

	@FastInject(method = "extractShadow", at = @At("HEAD"))
	private boolean suppress(S state, Minecraft minecraft, Level level) {
		return !(state instanceof MorphedPlayerRenderState.Holder holder) || holder.getState() == null;
	}
}
