package net.blockomorph.utils.accessors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface GuiGraphicsAccessor {
	ItemStackRenderState getItemStackRenderState$blockomorph();
	MultiBufferSource.BufferSource getBuffer$blockomorph();

	static GuiGraphicsAccessor of(GuiGraphics guiGraphics) {
		return (GuiGraphicsAccessor) guiGraphics;
	}
}
