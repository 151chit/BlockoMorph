package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.FastColor;

public class StringTagRenderer extends PrimitiveTagRenderer<StringTag> {

	public StringTagRenderer(String tagName, StringTag tag, TagRendererContext<StringTag> ctx) {
		super(tagName, tag, ctx);
		this.valueBox.setValue(tag.getAsString());
		this.valueBox.setTextColor(FastColor.ARGB32.color(43, 55, 224));
	}

	@Override
	protected Integer getPlateNumber() {
		return 0;
	}

	@Override
	protected void onValueEntered(String value) {
		this.changeThis(StringTag.valueOf(value));
	}
}
