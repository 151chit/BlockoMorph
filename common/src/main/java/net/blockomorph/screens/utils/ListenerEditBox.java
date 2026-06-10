package net.blockomorph.screens.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ListenerEditBox extends EditBox {
	public static final Identifier EDITBOX_BORDER_SPRITE = GuiUtils.res("textures/screens/editbox.png");
	public static final Identifier VANILLA = GuiUtils.vanillaRes("");
	private final Identifier borderTexture;
	private final GuiUtils gui = new GuiUtils();
	private final Font font;
	private final Consumer<String> action;
	protected boolean editable = true;
	private final boolean borderLock;
	private Predicate<String> filter;

	public ListenerEditBox(Font font, int x, int y, int length, int height, Component name, Consumer<String> action, @Nullable Identifier border) {
		super(font, x, y, length, height, name);
		this.action = action;
		this.font = font;
		this.borderTexture = border;
		this.setBordered(border != null);
		this.borderLock = true;
	}

	@Override
	public void setTextColor(int i) {
		super.setTextColor(this.convertColor(i));
	}

	@Override
	public void setTextColorUneditable(int i) {
		super.setTextColorUneditable(this.convertColor(i));
	}

	private int convertColor(int color) {
		if (ARGB.alpha(color) == 0) {
			color = (255 << 24) | color;
		}
		return color;
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		return this.check(() -> super.keyPressed(keyEvent));
	}

	@Override
	public boolean charTyped(CharacterEvent characterEvent) {
		return this.check(() -> super.charTyped(characterEvent));
	}

	private boolean check(BooleanSupplier input) {
		if (this.active && this.visible && this.editable && this.isFocused()) {
			String value = this.getValue();
			boolean flag = input.getAsBoolean();
			if (!value.equals(this.getValue())) this.action.accept(this.getValue());
			return flag;
		}
		return false;
	}


	@Override
	public void insertText(String input) {
		if (this.filter != null && !this.filter.test(input)) return;
		super.insertText(input);
	}

	@Override
	public void setValue(String value) {
		if (this.filter != null && !this.filter.test(value)) return;
		super.setValue(value);
	}

	@Override
	public void setEditable(boolean yes) {
		super.setEditable(yes);
		this.editable = yes;
	}

	@Override
	public boolean isBordered() {
		return this.borderTexture == VANILLA;
	}

	@Override
	public int getInnerWidth() {
		return this.getWidth() - 8;
	}

	@Override
	public void setBordered(boolean value) {
		if (!this.borderLock) {
			super.setBordered(value);
		}
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float ticks) {
		if (this.borderTexture != null && this.borderTexture != VANILLA) {
			this.gui.setGuiGraphicsExtractor(g, this.font, mouseX, mouseY, ticks);
			this.gui.blit(this.borderTexture, this.getX(), this.getY(), 0, this.editable ? 0 : this.getHeight(), this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight() * 2);
		}
		super.extractWidgetRenderState(g, mouseX, mouseY, ticks);
	}

	public void setFilter(Predicate<String> object) {
		this.filter = object;
	}

}
