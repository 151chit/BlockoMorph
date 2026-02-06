package net.blockomorph.screens.config.renderers;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.CategoryOption;
import net.minecraft.client.renderer.Rect2i;

public class CategoryConfigRenderer extends ButtonRightOption<CategoryOption> {

	public CategoryConfigRenderer() {
		super(125, 2, 16, 16, 144, 60);
	}

	@Override
	public void render(GuiUtils gui, CategoryOption configInstance, Rect2i box) {
	}

	@Override
	public boolean mouseScrolled(CategoryOption configInstance, double mouseX, double mouseY, double yOffsetWheel, Rect2i box, ConfigRenderingContext context) {
		return false;
	}

	@Override
	public void renderBackground(GuiUtils gui, CategoryOption configInstance, Rect2i box) {
		gui.blit(PLATES_SPRITE, box.getX(), box.getY(), 0, 60, 144, 20, MAX_X, MAX_Y);
		super.renderBackground(gui, configInstance, box);
	}

	@Override
	public void onClick(CategoryOption configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context) {
		context.onEnteringRequested().accept(configInstance);
	}
}
