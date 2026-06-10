package net.blockomorph.screens.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SpriteImageButton extends Button {
	private final GuiUtils utils = new GuiUtils();
	private final Identifier sprite;
	private final Supplier<Boolean> activated;
	private final boolean useInActive;
	private final int u, v, maxU, maxV;

	public SpriteImageButton(int x, int y, int lengthButtonOnScreen, int heightButtonOnScreen, Identifier sprite, OnPress onPress, @Nullable Supplier<Boolean> active, boolean useInActive) {
		super(x, y, lengthButtonOnScreen, heightButtonOnScreen, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.sprite = sprite;
		this.activated = active;
		this.useInActive = useInActive;
		this.u = 0;
		this.v = 0;
		this.maxU = this.getWidth();
		this.maxV = this.getHeight() * (useInActive ? 3 : 2);
	}

	public SpriteImageButton(int x, int y, int lengthButtonOnScreen, int heightButtonOnScreen, Identifier sprite, OnPress onPress, @Nullable Supplier<Boolean> active, boolean useInActive, int u, int v, int maxU, int maxV) {
		super(x, y, lengthButtonOnScreen, heightButtonOnScreen, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
		this.sprite = sprite;
		this.activated = active;
		this.useInActive = useInActive;
		this.u = u;
		this.v = v;
		this.maxV = maxV;
		this.maxU = maxU;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float delta) {
		if (this.activated != null) {
			this.active = this.activated.get();
		}
		utils.setGuiGraphicsExtractor(GuiGraphicsExtractor, Minecraft.getInstance().font, mouseX, mouseY, delta);
		int y = 0;
		if (!this.active) {
			if (this.useInActive) {
				y = this.getHeight() * 2;
			} else throw new IllegalArgumentException("Button disabled, but sprite not contains INACTIVE texture!");
		} else {
			if (this.isHovered) {
				y = this.getHeight();
			}
		}
		utils.blit(sprite, this.getX(), this.getY(), this.u, this.v + y, this.getWidth(), this.getHeight(), this.maxU, this.maxV);
	}
}
