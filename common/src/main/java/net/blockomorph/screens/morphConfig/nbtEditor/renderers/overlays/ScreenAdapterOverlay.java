package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays;

import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public class ScreenAdapterOverlay<SC extends Screen> extends TagEditingOverlay {
	protected final SC screen;

	public ScreenAdapterOverlay(SC screen) {
		super(getSize(true, screen), getSize(false, screen));
		this.screen = screen;
	}

	private static int getSize(boolean x, Screen screen) {
		if (screen instanceof AbstractScreen sc) {
			return x ? sc.imageLength : sc.imageHeight;
		}
		return 0;
	}

	@Override
	public void init(int width, int height, Consumer<TagEditingOverlay> onChange) {
		super.init(width, height, onChange);
		this.screen.init(GuiUtils.MC, width, height);
	}

	@Override
	public void renderInGui(GuiUtils gui) {
		this.screen.renderWithTooltip(gui.getGuiGraphics(), gui.getMouseX(), gui.getMouseY(), gui.getTick());
	}

	@Override
	protected void renderBackground(GuiUtils gui) {
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (this.screen.mouseClicked(mouseX, mouseY, type)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, type);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xWheelOffset, double yWheelOffset) {
		return this.screen.mouseScrolled(mouseX, mouseY, xWheelOffset, yWheelOffset);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int type, double mouseXOffset, double mouseYOffset) {
		return this.screen.mouseDragged(mouseX, mouseY, type, mouseXOffset, mouseYOffset);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int type) {
		return this.screen.mouseReleased(mouseX, mouseY, type);
	}

	@Override
	public boolean charTyped(char character, int mods) {
		return this.screen.charTyped(character, mods);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		return this.screen.keyPressed(key, scancode, mods);
	}
}
