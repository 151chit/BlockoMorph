package net.blockomorph.screens.config.renderers;

import net.blockomorph.screens.config.DamageTypeListOptionEditingScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.config.list.DamageTypeIdsConfig;
import net.minecraft.client.renderer.Rect2i;

public class DamageSourceOptionRenderer extends ButtonRightOption<DamageTypeIdsConfig> {
	public DamageSourceOptionRenderer() {
		super(125, 2, 16, 16, 144, 80);
	}

	@Override
	public void onClick(DamageTypeIdsConfig configInstance, double mouseX, double mouseY, Rect2i box, ConfigRenderingContext context) {
		GuiUtils.MC.setScreen(new DamageTypeListOptionEditingScreen(configInstance, context));
	}

	@Override
	public void render(GuiUtils gui, DamageTypeIdsConfig configInstance, Rect2i box) {

	}

	@Override
	public boolean mouseScrolled(DamageTypeIdsConfig configInstance, double mouseX, double mouseY, double yOffsetWheel, Rect2i box, ConfigRenderingContext context) {
		return false;
	}

	@Override
	public void renderBackground(GuiUtils gui, DamageTypeIdsConfig configInstance, Rect2i box) {
		gui.blit(PLATES_SPRITE, box.getX(), box.getY(), 0, 80, 144, 20, MAX_X, MAX_Y);
		super.renderBackground(gui, configInstance, box);
	}
}
