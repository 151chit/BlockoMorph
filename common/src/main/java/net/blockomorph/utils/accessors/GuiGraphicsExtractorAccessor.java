package net.blockomorph.utils.accessors;


import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GuiGraphicsExtractorAccessor {
	GuiRenderState getGuiState$blockomorph();

	static GuiGraphicsExtractorAccessor of(GuiGraphicsExtractor GuiGraphicsExtractor) {
		return (GuiGraphicsExtractorAccessor) GuiGraphicsExtractor;
	}
}
