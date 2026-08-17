package net.blockomorph.screens.overlay;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class PlayerCrackOverlay implements Overlay {
	private static final Function<Integer, ResourceLocation> PROGRESS = k -> GuiUtils.vanillaRes("textures/block/destroy_stage_" + k + ".png");

	@Override
	public void render(GuiUtils gui, int screenWidth, int screenHeight) {
		if (GuiUtils.MC.player instanceof PlayerAccessor player) {
			int progress = player.getBiggestDestroyProgress();
			if (progress >= 0 && progress < 10) {
				renderCracks(gui, progress, screenWidth, screenHeight);
			}
		}
	}

	public static void renderCracks(GuiUtils gui, int progress, int screenWidth, int screenHeight) {
		gui.blit(PROGRESS.apply(progress), 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
	}
}
