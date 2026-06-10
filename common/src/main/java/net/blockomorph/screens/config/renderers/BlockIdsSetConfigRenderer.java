package net.blockomorph.screens.config.renderers;

import net.blockomorph.screens.config.BlockListOptionEditingMorphScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.list.BlockIdsSetConfig;
import net.minecraft.client.renderer.Rect2i;

public class BlockIdsSetConfigRenderer extends ButtonRightOption<BlockIdsSetConfig> {
	public BlockIdsSetConfigRenderer() {
		super(125, 2, 16, 16, 144, 40);
	}

	@Override
	public void onClick(BlockIdsSetConfig configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context) {
		GuiUtils.MC.setScreen(new BlockListOptionEditingMorphScreen(configInstance, context));
	}

	@Override
	public void render(GuiUtils gui, BlockIdsSetConfig configInstance, Rect2i box) {
	}

	@Override
	public boolean mouseScrolled(BlockIdsSetConfig configInstance, double mouseX, double mouseY, double yOffsetWheel, Rect2i box, ConfigRenderingContext context) {
		return false;
	}

	@Override
	public void renderBackground(GuiUtils gui, BlockIdsSetConfig configInstance, Rect2i box) {
		gui.blit(PLATES_SPRITE, box.getX(), box.getY(), 0, 40, 144, 20, MAX_X, MAX_Y);
		super.renderBackground(gui, configInstance, box);
	}

	@Override
	public int getOptionColor(BlockIdsSetConfig configInstance) {
		return -6710887;
	}
}