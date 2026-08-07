package net.blockomorph.screens.overlay;

import com.mojang.blaze3d.platform.Window;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public interface Overlay {
	GuiUtils GUI_RENDERER = new GuiUtils();
	List<Overlay> OVERLAYS = List.of(new PlayerCrackOverlay(), new BlockHeartOverlay());

	void render(GuiUtils gui, int screenWidth, int screenHeight);

	static void renderOverlays(GuiGraphicsExtractor gui, float delta) {
		if (GuiUtils.MC.level != null) {
			GUI_RENDERER.setGuiGraphics(gui, GuiUtils.MC.font, 0, 0, delta);
			Window window = GuiUtils.MC.getWindow();
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < OVERLAYS.size(); i++) {
				OVERLAYS.get(i).render(GUI_RENDERER, window.getGuiScaledWidth(), window.getGuiScaledHeight());
			}
		}
	}
}
