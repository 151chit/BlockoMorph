package net.blockomorph.screens.morphConfig.propertiesWidget;

import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.Consumer;

import static net.blockomorph.screens.morphConfig.propertiesWidget.BlockStatePropsRenderer.PLATE_HEIGHT;

public interface PropertyRenderer<VALUE extends Comparable<VALUE>, T extends Property<VALUE>> {
	ResourceLocation PROPERTIES_SPRITE = GuiUtils.res("textures/screens/properties.png");
	int SPRITE_HEIGHT = 83;
	int SPRITE_LENGTH = 67;

	void renderBackground(GuiUtils gui, T property, VALUE value, Rect2i box);

	void render(GuiUtils gui, T property, VALUE value, Rect2i box);

	boolean mouseClicked(Consumer<BlockState> newStateHandler, BlockState state, T property, VALUE value, double mouseX, double mouseY, Rect2i box);

	boolean mouseScrolled(Consumer<BlockState> newStateHandler, BlockState state, T property, VALUE value, double mouseX, double mouseY, double yOffsetWheel, Rect2i box);

	default void renderPlate(GuiUtils gui, Rect2i box, int number) {
		gui.blit(PROPERTIES_SPRITE, box.getX(), box.getY(), 0, PLATE_HEIGHT * number, SPRITE_LENGTH, PLATE_HEIGHT, SPRITE_LENGTH, SPRITE_HEIGHT);
	}

	default void renderPropertyName(GuiUtils gui, T property, Rect2i box) {
		String name = property.getName();
		if (gui.getFont().width(name) > 31) {
			name = gui.getFont().plainSubstrByWidth(name, 29) + "..";
		}
		gui.drawString(Component.literal(name), box.getX() + 3, box.getY() + 6, -1, false);
	}

	//HINT: return true to brake tooltip render in all properties
	default boolean renderTooltip(GuiUtils gui, T property, VALUE value, Rect2i box) {
		if (GuiUtils.isMouseOver(box.getX(), box.getY(), box.getX() + 36, box.getY() + box.getHeight(), gui.getMouseX(), gui.getMouseY())) {
			if (gui.getFont().width(property.getName()) > 31) {
				gui.renderTooltip(Component.literal(property.getName()), gui.getMouseX(), gui.getMouseY());
			}
		}
		return false;
	}

	default void startClick() {
	}

	default void endClick() {
	}
}
