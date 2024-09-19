
package net.blockomorph.screens;

import net.blockomorph.utils.PlayerAccessor;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class PlayerCrackOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {

		    int k = ((PlayerAccessor)entity).getProgress();
		    
		    RenderSystem.disableDepthTest();
		    RenderSystem.depthMask(false);
		    RenderSystem.enableBlend();
		    RenderSystem.setShader(GameRenderer::getPositionTexShader);
		    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		    RenderSystem.setShaderColor(1, 1, 1, 1);
		    
			if (k >= 0 && k < 10) event.getGuiGraphics().blit(ResourceLocation.withDefaultNamespace("textures/block/destroy_stage_" + k + ".png"), 0, 0, 16, 16, w, h, w, h);

			RenderSystem.depthMask(true);
		    RenderSystem.defaultBlendFunc();
		    RenderSystem.enableDepthTest();
		    RenderSystem.disableBlend();
		    RenderSystem.setShaderColor(1, 1, 1, 1);
			
		}
	}
}
