package net.blockomorph.screens.morphConfig.propertiesWidget.renderers;

import net.blockomorph.screens.morphConfig.propertiesWidget.PropertyRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Consumer;

public class IntegerPropertyRenderer implements PropertyRenderer<Integer, IntegerProperty> {
	@Override
	public void renderBackground(GuiUtils gui, IntegerProperty property, Integer integer, Rect2i box) {
		this.renderPlate(gui, box, 1);
		gui.drawString(Component.literal(integer.toString()), box.getX() + 41, box.getY() + 6, -1, false);
	}

	@Override
	public void render(GuiUtils gui, IntegerProperty property, Integer integer, Rect2i box) {
	}

	@Override
	public boolean mouseClicked(Consumer<BlockState> newStateHandler, BlockState state, IntegerProperty property, Integer integer, double mouseX, double mouseY, Rect2i box) {
		return false;
	}

	@Override
	public boolean mouseScrolled(Consumer<BlockState> newStateHandler, BlockState state, IntegerProperty property, Integer integer, double mouseX, double mouseY, double yOffsetWheel, Rect2i box) {
		if (GuiUtils.isInBounds(box, mouseX, mouseY)) {
			int newValue = integer;
			if (yOffsetWheel < 0) {
				newValue--;
			} else if (yOffsetWheel > 0) {
				newValue++;
			}
			if (newValue != integer && property.getPossibleValues().contains(newValue)) {
				BlockState newState = state.trySetValue(property, newValue);
				newStateHandler.accept(newState);
				GuiUtils.playClickSound();
				return true;
			}
		}
		return false;
	}
}
