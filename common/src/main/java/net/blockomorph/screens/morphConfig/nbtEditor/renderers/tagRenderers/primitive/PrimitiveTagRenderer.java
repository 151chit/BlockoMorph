package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.TagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.function.BiFunction;

public abstract class PrimitiveTagRenderer<T extends PrimitiveTag> extends TagRenderer<T> {
	private static final EditBox.TextFormatter SHADOW_DISABLE = (value, cursorPos) -> {
		return FormattedCharSequence.forward(value, Style.EMPTY.withShadowColor(0));
	};
	protected final ListenerEditBox valueBox;

	public PrimitiveTagRenderer(String tagName, T tag, TagRendererContext<T> ctx) {
		super(tagName, tag, ctx);
		this.valueBox = new ListenerEditBox(GuiUtils.MC.font, 0, 0, 79, 11, Component.literal(tag.getType().getName() + " tag value"), this::onValueEntered, null);
		this.valueBox.setMaxLength(8166);
		this.valueBox.addFormatter(SHADOW_DISABLE);
	}

	@Override
	public void render(GuiUtils gui) {
		this.valueBox.setPosition(this.box.getX() + this.box.getWidth() - 81, this.box.getY() + 6);
		this.valueBox.render(gui.getGuiGraphics(), gui.getMouseX(), gui.getMouseY(), gui.getTick());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(0, 0));
		boolean result = this.valueBox.mouseClicked(event, false);
		this.valueBox.setFocused(result);
		return false;
	}

	@Override
	public boolean charTyped(char character, int mods) {
		CharacterEvent event = new CharacterEvent(character, mods);
		return this.valueBox.charTyped(event);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		KeyEvent event = new KeyEvent(key, scancode, mods);
		return this.valueBox.keyPressed(event);
	}

	@Override
	protected abstract Integer getPlateNumber();

	protected abstract void onValueEntered(String value);
}
