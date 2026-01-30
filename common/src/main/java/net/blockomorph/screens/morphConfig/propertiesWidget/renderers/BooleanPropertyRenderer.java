package net.blockomorph.screens.morphConfig.propertiesWidget.renderers;

import net.blockomorph.screens.morphConfig.propertiesWidget.PropertyRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Consumer;

public class BooleanPropertyRenderer implements PropertyRenderer<Boolean, BooleanProperty> {
	@Override
	public void renderBackground(GuiUtils gui, BooleanProperty property, Boolean aBoolean, Rect2i box) {
		this.renderPlate(gui, box, 2);
		if (aBoolean) {
			gui.blit(PROPERTIES_SPRITE, box.getX() + 44, box.getY() + 6, 0, 76, 15, 7, SPRITE_LENGTH, SPRITE_HEIGHT);
		}
	}

	@Override
	public void render(GuiUtils gui, BooleanProperty property, Boolean aBoolean, Rect2i box) {
	}

	@Override
	public boolean mouseClicked(Consumer<BlockState> newStateHandler, BlockState state, BooleanProperty property, Boolean aBoolean, double mouseX, double mouseY, Rect2i box) {
		if (GuiUtils.isInBounds(box, mouseX, mouseY)) {
			BlockState state2 = state.trySetValue(property, !aBoolean);
			newStateHandler.accept(state2);
			GuiUtils.playClickSound();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(Consumer<BlockState> newStateHandler, BlockState state, BooleanProperty property, Boolean aBoolean, double mouseX, double mouseY, double yOffsetWheel, Rect2i box) {
		return false;
	}
}
