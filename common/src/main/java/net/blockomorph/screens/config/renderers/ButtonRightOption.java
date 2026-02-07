package net.blockomorph.screens.config.renderers;

import net.blockomorph.screens.config.ConfigRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.client.renderer.Rect2i;

public abstract class ButtonRightOption<T extends ConfigInstance<?>> implements ConfigRenderer<T> {
	private final int[] data = new int[6];

	protected ButtonRightOption(int x, int y, int xSize, int ySize, int u, int v) {
		data[0] = x;
		data[1] = y;
		data[2] = xSize;
		data[3] = ySize;
		data[4] = u;
		data[5] = v;
	}

	@Override
	public void renderBackground(GuiUtils gui, T configInstance, Rect2i box) {
		int buttonX = box.getX() + data[0];
		int buttonY = box.getY() + data[1];
		boolean hovered = this.isButtonHovered(buttonX, buttonY, gui.getMouseX(), gui.getMouseY());
		if (hovered)
			gui.blit(PLATES_SPRITE, buttonX, buttonY, data[4], data[5], data[2], data[3], MAX_X, MAX_Y);
	}

	@Override
	public boolean mouseClicked(T configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context) {
		int buttonX = box.getX() + data[0];
		int buttonY = box.getY() + data[1];
		if (this.isButtonHovered(buttonX, buttonY, mouseX, mouseY)) {
			GuiUtils.playClickSound();
			this.onClick(configInstance, mouseX, mouseY, box, context);
			return true;
		}
		return false;
	}

	public abstract void onClick(T configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context);

	private boolean isButtonHovered(int buttonX, int buttonY, double mouseX, double mouseY) {
		return GuiUtils.isMouseOver(buttonX, buttonY, buttonX + data[2], buttonY + data[3], mouseX, mouseY);
	}

}
