package net.blockomorph.screens.config.renderers;

import net.blockomorph.screens.config.ConfigRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.BooleanConfig;
import net.minecraft.client.renderer.Rect2i;

public class BooleanConfigRenderer implements ConfigRenderer<BooleanConfig> {
	@Override
	public void renderBackground(GuiUtils gui, BooleanConfig configInstance, Rect2i box) {
		gui.blit(PLATES_SPRITE, box.getX(), box.getY(), 0, 0, 144, 20, MAX_X, MAX_Y);
		if (configInstance.getValue()) {
			gui.blit(PLATES_SPRITE, box.getX() + 105, box.getY() + 3, 144, 0, 24, 14, MAX_X, MAX_Y);
		}
	}

	@Override
	public void render(GuiUtils gui, BooleanConfig configInstance, Rect2i box) {
	}

	@Override
	public boolean mouseClicked(BooleanConfig configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context) {
		if (GuiUtils.isInBounds(box, mouseX, mouseY)) {
			boolean value = !configInstance.getValue();
			context.onValueChanged().accept(configInstance.getName(), value + "");
			GuiUtils.playClickSound();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(BooleanConfig configInstance, double mouseX, double mouseY, double yOffsetWheel, Rect2i box, ConfigRenderingContext context) {
		return false;
	}
}
