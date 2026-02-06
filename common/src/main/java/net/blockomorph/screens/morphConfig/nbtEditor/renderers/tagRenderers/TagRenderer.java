package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers.AbstractInterpritationTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class TagRenderer<T extends Tag> {
	protected static final int MAX_NAME_WIDTH = 53;
	public static final int PLATE_SPRITE_LENGTH = 194;
	public static final int PLATE_SPRITE_HEIGTH = 200;
	public static final int PLATE_LENGTH = 144;
	public static final int PLATE_HEIGTH = 20;
	protected static final ResourceLocation TAGS_SPRITE = GuiUtils.res("textures/screens/tags.png");
	protected final TagRendererContext<T> tagRendererContext;
	protected final Rect2i box = new Rect2i(0, 0, PLATE_LENGTH, PLATE_HEIGTH);
	private T tag;
	private final String name;
	private boolean nameVisibility = true;

	public TagRenderer(String tagName, T tag, TagRendererContext<T> ctx) {
		this.name = tagName;
		this.tag = tag;
		this.tagRendererContext = ctx;
	}

	public void setNameVisibility(boolean yes) {
		this.nameVisibility = yes;
	}

	protected boolean hasName() {
		return this.nameVisibility;
	}

	protected void enterInTag() {
		if (this.canEnterInTag())
			this.tagRendererContext.onEntering().accept(this.name, false);
	}

	protected void changeThis(T newSelf) {
		Objects.requireNonNullElse(this.tagRendererContext.onTagUpdate(), v -> {
		}).accept(newSelf);
		this.tag = newSelf;
		this.tagRendererContext.onMainTagEdited().run();
	}

	public void forceTagChange(T newSelf) {
		this.tag = newSelf;
	}

	public void renderPlateWithoutCtx(GuiUtils gui) {
		if (this.getPlateNumber() != null) this.renderPlate(gui, this.getPlateNumber());
	}

	protected void renderPlate(GuiUtils gui, int number) {
		gui.blit(TAGS_SPRITE, this.box.getX(), this.box.getY(), 0, PLATE_HEIGTH * number, PLATE_LENGTH, PLATE_HEIGTH, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
		this.renderLine(gui, number);
	}

	protected void renderLine(GuiUtils gui, int number) {
		if (!this.nameVisibility)
			gui.blit(TAGS_SPRITE, this.box.getX() + 57, this.box.getY(), 55, PLATE_HEIGTH * number, 2, PLATE_HEIGTH, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
	}

	protected void renderDynamicColorLine(GuiUtils gui) {
		gui.blit(TAGS_SPRITE, this.box.getX() + 57, this.box.getY(), PLATE_LENGTH + 32 + (this.nameVisibility ? 0 : 2), 0, 2, PLATE_HEIGTH, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
	}

	protected void drawTextPart(GuiUtils gui) {
		if (this.nameVisibility) {
			String name = gui.getFont().plainSubstrByWidth(this.name, MAX_NAME_WIDTH) + ((gui.getFont().width(this.name) > MAX_NAME_WIDTH + 5) ? ".." : "");
			gui.drawString(Component.literal(name), this.box.getX() + 4, this.box.getY() + 5, -1, false);
		}
	}

	@Nullable
	public Component getTooltip(GuiUtils gui) {
		if (GuiUtils.isMouseOver(this.box.getX(), this.box.getY(), this.box.getX() + MAX_NAME_WIDTH + 5, this.box.getY() + this.box.getHeight(), gui.getMouseX(), gui.getMouseY())) {
			if (gui.getFont().width(this.name) > MAX_NAME_WIDTH + 5) {
				return Component.literal(this.name);
			}
		}
		return null;
	}

	public T getTag() {
		return this.tag;
	}

	public String getName() {
		return this.name;
	}

	public Rect2i getBox() {
		return this.box;
	}

	public final void renderTag(GuiUtils gui) {
		Integer number = this.getPlateNumber();
		if (number != null) {
			this.renderPlate(gui, number);
		}
		this.drawTextPart(gui);
		this.render(gui);
	}

	public abstract void render(GuiUtils gui);

	public boolean mouseClicked(double mouseX, double mouseY) {
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double yOffsetWheel) {
		return false;
	}

	public boolean keyPressed(int key, int scancode, int mods) {
		return false;
	}

	public boolean charTyped(char character, int mods) {
		return false;
	}

	public boolean canEnterInTag() {
		return false;
	}

	public int getFrameColor() {
		return -1;
	}

	@Nullable
	protected Integer getPlateNumber() {
		return null;
	}

	public List<TagRenderer<?>> getEnteringTags() {
		return List.of();
	}

	public Tag tryWalk(String nameElementInThisTag) {
		return null;
	}

	@Nullable
	public TagEditingOverlay getTagAddOverlay() {
		return null;
	}

	public void deleteTag(String name) {
	}

	@Nullable
	public AbstractInterpritationTagRenderer<T> getInterpretationRenderer(Runnable onInterpretationBrake) {
		return null;
	}
}
