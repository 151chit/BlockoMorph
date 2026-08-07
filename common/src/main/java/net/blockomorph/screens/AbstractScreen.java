package net.blockomorph.screens;

import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class AbstractScreen extends Screen {
	public final GuiUtils gui = new GuiUtils();
	public final int imageLength;
	public final int imageHeight;
	protected int leftPos;
	protected int topPos;
	private final Identifier MENU;

	protected AbstractScreen(String id, /* screen length/height */ @Nullable ScreenPosition size) {
		super(Component.literal(id));
		MENU = GuiUtils.res("textures/screens/" + id + ".png");
		if (size == null) {
			this.imageLength = 176;
			this.imageHeight = 166;
		} else {
			this.imageLength = size.x();
			this.imageHeight = size.y();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float tick) {
		super.extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, tick);
	}

	@Override
	public final boolean mouseScrolled(double mouseX, double mouseY, double xScrollOffset, double yScrollOffset) {
		return this.mouseScrolled(mouseX, mouseY, yScrollOffset);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double yScrollOffset) {
		return super.mouseScrolled(mouseX, mouseY, 0, yScrollOffset);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float tick) {
		this.gui.setGuiGraphics(GuiGraphicsExtractor, this.font, mouseX, mouseY, tick);
		super.extractBackground(GuiGraphicsExtractor, mouseX, mouseY, tick);
		this.renderMenu();
	}

	public Font getFont() {
		return this.font;
	}

	protected void renderMenu() {
		this.gui.blitMonoImage(MENU, this.leftPos, this.topPos, this.imageLength, this.imageHeight);
	}

	@Override
	protected void init() {
		this.leftPos = (this.width - this.imageLength) / 2;
		this.topPos = (this.height - this.imageHeight) / 2;
	}
}
