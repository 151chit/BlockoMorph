package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays;

import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class TagEditingOverlay extends AbstractContainerEventHandler {
	protected int width;
	protected int height;
	protected int imageLength;
	protected int imageHeight;
	protected Consumer<TagEditingOverlay> onChange;
	private final List<AbstractWidget> widgets = new ArrayList<>();

	public TagEditingOverlay(int imageLength, int imageHeigth) {
		this.imageLength = imageLength;
		this.imageHeight = imageHeigth;
	}

	public void init(int width, int height, Consumer<TagEditingOverlay> onChange) {
		this.widgets.clear();
		this.width = width;
		this.height = height;
		this.onChange = onChange;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
		if (this.closeOnEmptyClick(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button())) {
			this.onChange.accept(null);
			return true;
		}
		return super.mouseClicked(mouseButtonEvent, doubleClick);
	}

	protected boolean closeOnEmptyClick(double mouseX, double mouseY, int type) {
		if (type == 0) {
			int x = this.width / 2 - this.imageLength / 2;
			int y = this.height / 2 - this.imageHeight / 2;
			return !GuiUtils.isMouseOver(x, y, x + this.imageLength, y + this.imageHeight, mouseX, mouseY);
		}
		return false;
	}

	public final void render(GuiUtils gui) {
		this.renderBackground(gui);
		this.widgets.forEach(widget -> {
			widget.extractRenderState(gui.getGuiGraphicsExtractor(), gui.getMouseX(), gui.getMouseY(), gui.getTick());
		});
		this.renderInGui(gui);
	}

	protected abstract void renderInGui(GuiUtils gui);

	protected abstract void renderBackground(GuiUtils gui);

	@Override
	public List<? extends GuiEventListener> children() {
		return this.widgets;
	}

	protected void addRenderableWidget(AbstractWidget widget) {
		this.widgets.add(widget);
	}
}
