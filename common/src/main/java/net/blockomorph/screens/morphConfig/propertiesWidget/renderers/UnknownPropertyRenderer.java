package net.blockomorph.screens.morphConfig.propertiesWidget.renderers;

import net.blockomorph.screens.morphConfig.propertiesWidget.PropertyRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ObjectListRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.function.Consumer;

public class UnknownPropertyRenderer<T extends Comparable<T>> implements PropertyRenderer<T, Property<T>> {
	private final ObjectListRenderer<Property<T>, T> RENDERER = new ObjectListRenderer<>(-1, -256, 7, Property::getName, property -> {
		return new ArrayList<>(property.getPossibleValues());
	});
	private boolean notClose;

	@Override
	public void renderBackground(GuiUtils gui, Property<T> property, T value, Rect2i box) {
		this.renderPlate(gui, box, 3);
		RENDERER.renderName(gui, box.getX() + 41, box.getY() + 6, 20, property, value, -1);
	}

	@Override
	public void render(GuiUtils gui, Property<T> property, T value, Rect2i box) {
		if (RENDERER.isCurrentProperty(property)) {
			RENDERER.render(gui, box.getX() + 41, box.getY() + 6, -1, 8);
		}
	}

	@Override
	public boolean mouseClicked(Consumer<BlockState> newStateHandler, BlockState state, Property<T> property, T t, double mouseX, double mouseY, Rect2i box) {
		if (RENDERER.isCurrentProperty(property) && RENDERER.mouseClicked(mouseX, mouseY)) {
			return true;
		} else if (GuiUtils.isInBounds(box, mouseX, mouseY)) {
			RENDERER.drop(property, newValue -> {
				GuiUtils.playClickSound();
				BlockState newState = state.trySetValue(property, newValue);
				newStateHandler.accept(newState);
			});
			GuiUtils.playClickSound();
			this.notClose = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(Consumer<BlockState> newStateHandler, BlockState state, Property<T> property, T t, double mouseX, double mouseY, double yOffsetWheel, Rect2i box) {
		if (RENDERER.isCurrentProperty(property)) {
			return RENDERER.mouseScrolled(mouseX, mouseY, yOffsetWheel);
		}
		return false;
	}

	@Override
	public boolean renderTooltip(GuiUtils gui, Property<T> property, T value, Rect2i box) {
		PropertyRenderer.super.renderTooltip(gui, property, value, box);
		if (RENDERER.isListFocused(gui.getMouseX(), gui.getMouseY())) {
			return RENDERER.isCurrentProperty(property);
		} else if (GuiUtils.isMouseOver(box.getX() + 36, box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), gui.getMouseX(), gui.getMouseY())) {
			String name = property.getName(value);
			if (gui.getFont().width(name) > 20) {
				gui.renderTooltip(Component.literal(name), gui.getMouseX(), gui.getMouseY());
			}
		}
		return false;
	}

	@Override
	public void startClick() {
		this.notClose = false;
	}

	@Override
	public void endClick() {
		if (!this.notClose) {
			RENDERER.close();
		}
	}
}
