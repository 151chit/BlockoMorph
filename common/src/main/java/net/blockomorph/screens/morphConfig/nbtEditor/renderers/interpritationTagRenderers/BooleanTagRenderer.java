package net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive.NumericTagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.nbt.ByteTag;

public class BooleanTagRenderer extends AbstractInterpritationTagRenderer<ByteTag> {
	public BooleanTagRenderer(NumericTagRenderer.ByteTagRenderer parent, TagRendererContext<ByteTag> ctx) {
		super(parent, ctx);
	}

	@Override
	public void renderMain(GuiUtils gui) {
		if (this.getTag().getAsByte() != 0) {
			gui.blit(TAGS_SPRITE, this.box.getX() + 95, this.box.getY() + 3, PLATE_LENGTH, 32, 24, 14, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
		}
	}

	@Override
	public boolean mouseClick(double mouseX, double mouseY) {
		if (GuiUtils.isInBounds(this.box, mouseX, mouseY)) {
			ByteTag tag = ByteTag.valueOf((byte) (this.getTag().getAsByte() != 0 ? 0 : 1));
			this.changeThis(tag);
			GuiUtils.playClickSound();
			return true;
		}
		return false;
	}

	@Override
	protected Integer getPlateNumber() {
		return 4;
	}
}
