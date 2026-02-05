package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.TagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.BiFunction;

public abstract class PrimitiveTagRenderer<T extends PrimitiveTag> extends TagRenderer<T> {
	private static final BiFunction<String, Integer, FormattedCharSequence> SHADOW_DISABLE = (value, cursorPos) -> {
		return FormattedCharSequence.forward(value, Style.EMPTY.withShadowColor(0));
	};
	protected final ListenerEditBox valueBox;

	public PrimitiveTagRenderer(String tagName, T tag, TagRendererContext<T> ctx) {
		super(tagName, tag, ctx);
		this.valueBox = new ListenerEditBox(GuiUtils.MC.font, 0, 0, 79, 11, Component.literal(tag.getType().getName() + " tag value"), this::onValueEntered, null);
		this.valueBox.setMaxLength(8166);
		this.valueBox.setFormatter(SHADOW_DISABLE);
	}

	@Override
	public void render(GuiUtils gui) {
		this.valueBox.setPosition(this.box.getX() + this.box.getWidth() - 81, this.box.getY() + 6);
		this.valueBox.render(gui.getGuiGraphics(), gui.getMouseX(), gui.getMouseY(), gui.getTick());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		boolean result = this.valueBox.mouseClicked(mouseX, mouseY, 0);
		this.valueBox.setFocused(result);
		return false;
	}

	@Override
	public boolean charTyped(char character, int mods) {
		return this.valueBox.charTyped(character, mods);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		return this.valueBox.keyPressed(key, scancode, mods);
	}

	@Override
	protected abstract Integer getPlateNumber();

	protected abstract void onValueEntered(String value);
}
