
package net.blockomorph.screens;

import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.GuiGraphics;

public class PlayerCrackOverlay {
	public static void render(GuiGraphics guiGraphics, float tickDelta) {
		int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
		    RenderSystem.disableDepthTest();
		    RenderSystem.depthMask(false);
		    RenderSystem.enableBlend();
		    RenderSystem.setShader(GameRenderer::getPositionTexShader);
		    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		    RenderSystem.setShaderColor(1, 1, 1, 1);

		    int k = ((PlayerAccessor)entity).getProgress();
		    
			if (k >= 0 && k < 10) guiGraphics.blit(new ResourceLocation("minecraft:textures/block/destroy_stage_" + k + ".png"), 0, 0, 16, 16, w, h, w, h);
			
		    RenderSystem.depthMask(true);
		    RenderSystem.defaultBlendFunc();
		    RenderSystem.enableDepthTest();
		    RenderSystem.disableBlend();
		    RenderSystem.setShaderColor(1, 1, 1, 1);
		}
	}
}
