package net.blockomorph.screens.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ListenerEditBox extends EditBox {
	public static final ResourceLocation EDITBOX_BORDER_SPRITE = GuiUtils.res("textures/screens/editbox.png");
	public static final ResourceLocation VANILLA = GuiUtils.vanillaRes("");
	private final ResourceLocation borderTexture;
	private final GuiUtils gui = new GuiUtils();
	private final Font font;
	private final Consumer<String> action;
	protected boolean editable = true;
	private final boolean borderLock;

	public ListenerEditBox(Font font, int x, int y, int length, int height, Component name, Consumer<String> action, @Nullable ResourceLocation border) {
		super(font, x, y, length, height, name);
		this.action = action;
		this.font = font;
		this.borderTexture = border;
		this.setBordered(border != null);
		this.borderLock = true;
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		return this.check(() -> super.keyPressed(key, scancode, mods));
	}

	@Override
	public boolean charTyped(char character, int mods) {
		return this.check(() -> super.charTyped(character, mods));
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
	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float ticks) {
		if (this.borderTexture != null && this.borderTexture != VANILLA) {
			this.gui.setGuiGraphics(g, this.font, mouseX, mouseY, ticks);
			this.gui.blit(this.borderTexture, this.getX(), this.getY(), 0, this.editable ? 0 : this.getHeight(), this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight() * 2);
		}
		super.renderWidget(g, mouseX, mouseY, ticks);
	}

}
