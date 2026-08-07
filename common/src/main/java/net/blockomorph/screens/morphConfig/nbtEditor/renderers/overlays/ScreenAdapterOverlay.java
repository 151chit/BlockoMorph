package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays;

import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

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
		this.screen.init(width, height);
	}

	@Override
	public void renderInGui(GuiUtils gui) {
		this.screen.extractRenderStateWithTooltipAndSubtitles(gui.getGuiGraphics(), gui.getMouseX(), gui.getMouseY(), gui.getTick());
	}

	@Override
	protected void renderBackground(GuiUtils gui) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
		if (this.screen.mouseClicked(mouseButtonEvent, doubleClick)) {
			return true;
		}
		return super.mouseClicked(mouseButtonEvent, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xWheelOffset, double yWheelOffset) {
		return this.screen.mouseScrolled(mouseX, mouseY, xWheelOffset, yWheelOffset);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double mouseXOffset, double mouseYOffset) {
		return this.screen.mouseDragged(mouseButtonEvent, mouseXOffset, mouseYOffset);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
		return this.screen.mouseReleased(mouseButtonEvent);
	}

	@Override
	public boolean charTyped(CharacterEvent characterEvent) {
		return this.screen.charTyped(characterEvent);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		return this.screen.keyPressed(keyEvent);
	}
}
