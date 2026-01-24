package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers.BooleanTagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.nbt.*;
import net.minecraft.util.FastColor;

import java.util.List;

public abstract class NumericTagRenderer<NUMBER_TAG extends NumericTag> extends PrimitiveTagRenderer<NUMBER_TAG> {
	public static final int INT_COLOR = FastColor.ARGB32.color(255, 89, 47, 245);
	public static final int LONG_COLOR = FastColor.ARGB32.color(255, 0, 137, 158);
	public static final int DOUBLE_COLOR = FastColor.ARGB32.color(255, 27, 206, 62);
	public static final int FLOAT_COLOR = FastColor.ARGB32.color(255, 250, 202, 32);
	public static final int SHORT_COLOR = FastColor.ARGB32.color(255, 173, 0, 184);
	public static final int BYTE_COLOR = FastColor.ARGB32.color(255, 178, 0, 20);
	private final NUMBER_TAG DEFAULT;
	private final int color;

	protected NumericTagRenderer(String tagName, NUMBER_TAG tag, TagRendererContext<NUMBER_TAG> ctx, NUMBER_TAG defaultValue, int backgroundColor) {
		super(tagName, tag, ctx);
		this.valueBox.setFilter(value -> {
			if (value.isEmpty() || value.equals("-")) return true;
			NUMBER_TAG output = this.parseInternal(value);
			return output != null;
		});
		DEFAULT = defaultValue;
		this.color = backgroundColor;
		this.valueBox.setValue(tag.getAsNumber().toString());
	}

	@Override
	public void renderPlate(GuiUtils gui, int number) {
		gui.fill(this.box.getX(), this.box.getY(), this.box.getX() + this.box.getWidth(), this.box.getY() + this.box.getHeight(), this.color);
		super.renderPlate(gui, number);
	}

	@Override
	protected void renderLine(GuiUtils gui, int number) {
		this.renderDynamicColorLine(gui);
	}

	@Override
	protected Integer getPlateNumber() {
		return 1;
	}

	@Override
	protected void onValueEntered(String value) {
		if (value.isEmpty() || value.equals("-")) this.changeThis(DEFAULT);
		NUMBER_TAG tag = this.parseInternal(value);
		if (tag != null) {
			this.changeThis(tag);
		}
	}

	private NUMBER_TAG parseInternal(String value) {
		try {
			return this.parse(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	protected abstract NUMBER_TAG parse(String value) throws NumberFormatException;


	public static class IntTagRenderer extends NumericTagRenderer<IntTag> {
		public IntTagRenderer(String tagName, IntTag tag, TagRendererContext<IntTag> ctx) {
			super(tagName, tag, ctx, IntTag.valueOf(0), INT_COLOR);
		}

		@Override
		protected IntTag parse(String value) throws NumberFormatException {
			return IntTag.valueOf(Integer.parseInt(value));
		}
	}

	public static class LongTagRenderer extends NumericTagRenderer<LongTag> {

		public LongTagRenderer(String tagName, LongTag tag, TagRendererContext<LongTag> ctx) {
			super(tagName, tag, ctx, LongTag.valueOf(0), LONG_COLOR);
		}

		@Override
		protected LongTag parse(String value) throws NumberFormatException {
			return LongTag.valueOf(Long.parseLong(value));
		}
	}

	public static class DoubleTagRenderer extends NumericTagRenderer<DoubleTag> {

		public DoubleTagRenderer(String tagName, DoubleTag tag, TagRendererContext<DoubleTag> ctx) {
			super(tagName, tag, ctx, DoubleTag.valueOf(0), DOUBLE_COLOR);
		}

		@Override
		protected DoubleTag parse(String value) throws NumberFormatException {
			return DoubleTag.valueOf(Double.parseDouble(value));
		}
	}

	public static class FloatTagRenderer extends NumericTagRenderer<FloatTag> {

		public FloatTagRenderer(String tagName, FloatTag tag, TagRendererContext<FloatTag> ctx) {
			super(tagName, tag, ctx, FloatTag.valueOf(0), FLOAT_COLOR);
		}

		@Override
		protected FloatTag parse(String value) throws NumberFormatException {
			return FloatTag.valueOf(Float.parseFloat(value));
		}
	}

	public static class ShortTagRenderer extends NumericTagRenderer<ShortTag> {

		public ShortTagRenderer(String tagName, ShortTag tag, TagRendererContext<ShortTag> ctx) {
			super(tagName, tag, ctx, ShortTag.valueOf((short) 0), SHORT_COLOR);
		}

		@Override
		protected ShortTag parse(String value) throws NumberFormatException {
			return ShortTag.valueOf(Short.parseShort(value));
		}
	}

	public static class ByteTagRenderer extends NumericTagRenderer<ByteTag> {
		private static final List<String> BOOLEAN_WORDS = List.of("is", "can", "has", "enable", "active", "power", "disable", "work", "in", "show");

		public ByteTagRenderer(String tagName, ByteTag tag, TagRendererContext<ByteTag> ctx) {
			super(tagName, tag, ctx, ByteTag.valueOf((byte) 0), BYTE_COLOR);
		}

		@Override
		protected ByteTag parse(String value) throws NumberFormatException {
			return ByteTag.valueOf(Byte.parseByte(value));
		}

		@Override
		public BooleanTagRenderer getInterpretationRenderer(Runnable onInterpretationBrake) {
			byte value = this.getTag().getAsByte();
			if (value == 0 || value == 1) {
				boolean yes = false;
				String name = this.getName().toLowerCase();
				for (String word : BOOLEAN_WORDS) {
					if (name.contains(word)) {
						yes = true;
						break;
					}
				}
				if (yes || name.endsWith("ed")) {
					return new BooleanTagRenderer(this, this.tagRendererContext.forInterpretation(onInterpretationBrake));
				}
			}
			return null;
		}
	}

}
