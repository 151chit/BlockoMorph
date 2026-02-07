package net.blockomorph.screens.overlay;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class PlayerCrackOverlay implements Overlay {
	private static final Function<Integer, Identifier> PROGRESS = k -> GuiUtils.vanillaRes("textures/block/destroy_stage_" + k + ".png");

	@Override
	public void render(GuiUtils gui, int screenWidth, int screenHeight) {
		if (GuiUtils.MC.player instanceof PlayerAccessor player) {
			int progress = player.getBiggestProgress();
			if (progress >= 0 && progress < 10) {
				gui.blit(PROGRESS.apply(progress), 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
			}
		}
	}
}
