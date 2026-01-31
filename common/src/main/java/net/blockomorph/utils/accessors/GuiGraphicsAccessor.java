package net.blockomorph.utils.accessors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface GuiGraphicsAccessor {
	GuiRenderState getGuiState$blockomorph();

	static GuiGraphicsAccessor of(GuiGraphics guiGraphics) {
		return (GuiGraphicsAccessor) guiGraphics;
	}
}
