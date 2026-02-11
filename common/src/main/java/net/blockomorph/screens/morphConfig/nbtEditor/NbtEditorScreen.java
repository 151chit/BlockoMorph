package net.blockomorph.screens.morphConfig.nbtEditor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.TagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.blockomorph.screens.utils.ScrollerManager;
import net.blockomorph.screens.utils.SpriteImageButton;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class NbtEditorScreen extends AbstractScreen {
	private static final ScrollerManager.CustomBarData SCOLLER = new ScrollerManager.CustomBarData(GuiUtils.res("textures/screens/nbt_scroller.png"), 7, 15);
	public static final ResourceLocation BUTTONS_SPRITE = GuiUtils.res("textures/screens/nbt_buttons.png");
	public static final ResourceLocation LOCK_IMAGE = GuiUtils.res("textures/screens/nbt_lock.png");
	public static final int BUTTON_SPRITE_LENGTH = 46;
	public static final int BUTTON_SPRITE_HEIGHT = 43;
	private static final int MAX_PLATES_COUNT = 7;
	private boolean initialized;
	private final BiConsumer<String, Boolean> onEntering = (tagName, intr) -> {
		this.enterInTag(this.path.append(tagName), intr);
	};
	private CompoundTag editingTag;
	private final Runnable onTagEdited;
	private EditBox tagBox;
	private Button pathExit;
	private Button addButton;
	private Button restoreButton;
	private int internalBoxX;
	private int internalBoxY;
	protected final int internalBoxLength = 173;
	protected final int internalBoxHeigth = 159;
	private final List<RenderableTag<?>> renderables = new ArrayList<>(MAX_PLATES_COUNT) {
		@Override
		public boolean add(RenderableTag<?> renderableTag) {
			boolean result = super.add(renderableTag);
			NbtEditorScreen.this.reinitDeleteButtons();
			return result;
		}

		@Override
		public void clear() {
			super.clear();
			NbtEditorScreen.this.reinitDeleteButtons();
		}
	};
	private final ScrollerManager<RenderableTag<?>> scrollerManager;
	private NbtPath path = new NbtPath.RootNbtPath();
	private TagRenderer<?> currentEnteredTag;
	private TagEditingOverlay overlay;
	private final Consumer<TagEditingOverlay> onTagReceived;
	private final List<SpriteImageButton> deleteButtons = new ArrayList<>(MAX_PLATES_COUNT);
	private String parseError;
	private boolean editorLocked;

	protected NbtEditorScreen() {
		super("nbt_editor_screen", new ScreenPosition(204, 191));
		this.scrollerManager = new ScrollerManager<>(() -> this.leftPos + 181, () -> this.topPos + 41, 138, 1, MAX_PLATES_COUNT, this.renderables, SCOLLER);
		this.onTagEdited = () -> {
			this.tagBox.setValue(this.editingTag.toString());
			this.onTagEdited(this.editingTag.copy());
		};
		this.onTagReceived = this::setOverlay;
	}

	public void setNewTag(CompoundTag tag) {
		if (!this.initialized) return;
		this.overlay = null;
		if (tag != null) {
			this.editingTag = tag.copy();
			this.tagBox.setValue(this.editingTag.toString());
			if (!this.editorLocked) {
				TagRenderer<?> renderer = this.getTagByPath();
				if (renderer == null || !renderer.canEnterInTag()) {
					this.path = new NbtPath.RootNbtPath();
				}
				this.initList(false);
			}
		} else {
			this.editingTag = null;
			this.scrollerManager.setMainList(new ArrayList<>());
			this.scrollerManager.setScrollOffset(0f);
			this.scrollerManager.refreshList();
			this.tagBox.setValue("");
			this.pathExit.visible = false;
		}
		this.parseError = null;
	}

	private void setOverlay(TagEditingOverlay tagEditor) {
		this.overlay = tagEditor;
		if (tagEditor != null) {
			tagEditor.init(this.width, this.height, this::setOverlay);
		}
	}

	protected abstract void onTagEdited(CompoundTag tag);

	protected int getWaitingColor() {
		return FastColor.ARGB32.color(255, 198, 198, 198);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
		super.render(guiGraphics, mouseX, mouseY, tick);
		if (this.editingTag == null) {
			this.gui.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + imageLength - 4, this.topPos + this.imageHeight - 4, this.getWaitingColor());
			int x = this.leftPos + this.imageLength / 2;
			int y = this.topPos + this.imageHeight / 2;
			this.gui.drawCenteredString(Component.translatable("blockomorph.gui.nbtEditor.waitingTag"), x, y, -1, true);
			this.gui.drawCenteredString(Component.literal(LoadingDotsText.get(Util.getMillis())), x, y + 10, -8355712, true);
			return;
		}
		for (Button button : this.deleteButtons) {
			button.render(guiGraphics, mouseX, mouseY, tick);
		}
		int i = 0;
		Component tooltip = null;
		for (RenderableTag<?> tag : this.renderables) {
			tag.render(this.gui, i * TagRenderer.PLATE_HEIGTH);
			i++;
			if (tooltip == null) {
				tooltip = tag.getTooltip(this.gui);
			}
		}
		this.renderError();
		if (this.overlay != null) {
			this.gui.renderInDepthIfNeededAfterBlockRendering(() -> {
				this.gui.blurScreen(this.width, this.height, 190);
				this.overlay.render(this.gui);
			});
		} else if (tooltip != null) {
			this.gui.renderTooltip(tooltip, mouseX, mouseY);
		}
	}

	private void renderError() {
		if (this.parseError != null) {
			int y = this.topPos + this.imageHeight;
			if (this.font.width(this.parseError) > this.width) {
				int textLength = this.font.width(this.parseError);
				int x = -(textLength - this.width);
				this.gui.fill(0, y, this.width, y + 11, Integer.MIN_VALUE);
				this.gui.drawString(Component.literal(this.parseError).withStyle(ChatFormatting.RED), x, y + 2, -1, true);
			} else {
				this.gui.drawCenteredStringWithAdditional(Component.literal(this.parseError).withStyle(ChatFormatting.RED), this.width / 2, y + 2, -1, false, (x, length) -> {
					this.gui.fill(x - 1, y, x + length + 1, y + 11, Integer.MIN_VALUE);
				});
			}
		}
	}

	public void setEditorLocked(boolean yes) {
		this.editorLocked = yes;
		if (yes) {
			this.path = new NbtPath.RootNbtPath();
			this.pathExit.visible = false;
			this.scrollerManager.setMainList(new ArrayList<>());
			this.scrollerManager.setScrollOffset(0f);
			this.scrollerManager.refreshList();
		} else {
			this.enterInTag(new NbtPath.RootNbtPath(), false);
		}
		this.addButton.visible = !yes;
		this.restoreButton.visible = yes && this.editingTag != null;
	}

	public void setError(String errorMessage) {
		this.parseError = errorMessage;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (this.editingTag == null) return false;
		if (this.overlay != null) {
			return this.overlay.mouseClicked(mouseX, mouseY, type);
		} else if (type == 0) {
			if (this.forEachTag(renderer -> renderer.mouseClicked(mouseX, mouseY))) {
				this.tagBox.setFocused(false);
				return true;
			}
			for (Button button : this.deleteButtons) {
				if (button.mouseClicked(mouseX, mouseY, type)) {
					this.tagBox.setFocused(false);
					return true;
				}
			}
		}
		this.tagBox.setFocused(this.tagBox.isMouseOver(mouseX, mouseY));
		if (super.mouseClicked(mouseX, mouseY, type)) {
			return true;
		} else return this.scrollerManager.mouseClicked(mouseX, mouseY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int type, double mouseXOffset, double mouseYOffset) {
		if (this.editingTag == null) return false;
		if (this.overlay != null) {
			return this.overlay.mouseDragged(mouseX, mouseY, type, mouseXOffset, mouseYOffset);
		} else if (this.scrollerManager.mouseDragged(mouseY)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, type, mouseXOffset, mouseYOffset);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double yWheelOffset) {
		if (this.editingTag == null) return false;
		if (this.overlay != null) {
			return this.overlay.mouseScrolled(mouseX, mouseY, yWheelOffset);
		} else if (this.forEachTag(renderer -> renderer.mouseScrolled(mouseX, mouseY, yWheelOffset))) {
			return true;
		}
		return this.scrollerManager.mouseScrolled(yWheelOffset);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int type) {
		if (this.editingTag == null) return false;
		this.scrollerManager.disableScrollWork();
		if (this.overlay != null) {
			this.overlay.mouseReleased(mouseX, mouseY, type);
		}
		return super.mouseReleased(mouseX, mouseY, type);
	}

	@Override
	public boolean charTyped(char character, int mods) {
		if (this.editingTag == null) return false;
		if (this.overlay != null) {
			return this.overlay.charTyped(character, mods);
		} else if (this.forEachTag(renderer -> renderer.charTyped(character, mods))) {
			return true;
		}
		return super.charTyped(character, mods);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (this.editingTag == null) {
			if (key == 256 && this.shouldCloseOnEsc()) {
				this.onClose();
				return true;
			}
			return false;
		}
		if (this.overlay != null) {
			if (key == 256) {
				this.onTagReceived.accept(null);
				return true;
			}
			return this.overlay.keyPressed(key, scancode, mods);
		} else if (this.forEachTag(renderer -> renderer.keyPressed(key, scancode, mods))) {
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	private boolean forEachTag(Function<RenderableTag<?>, Boolean> action) {
		for (RenderableTag<?> renderer : this.renderables) {
			if (action.apply(renderer)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void renderMenu() {
		int color = this.path.isEmpty() ? FastColor.ARGB32.color(255, 84, 84, 84) : this.currentEnteredTag.getFrameColor();
		this.gui.fill(this.internalBoxX, this.internalBoxY, this.internalBoxX + this.internalBoxLength, this.internalBoxY + this.internalBoxHeigth, color);
		super.renderMenu();
		this.scrollerManager.renderScroller(this.gui);
		this.gui.drawString(Component.literal(this.path.getVisualString()), this.internalBoxX + 11, this.internalBoxY + 5, -1, false);
		if (this.editorLocked) {
			this.gui.blitMonoImage(LOCK_IMAGE, this.internalBoxX, this.internalBoxY, this.internalBoxLength, this.internalBoxHeigth);
		}
	}

	private void parseTagboxInput(String value) {
		if (this.editingTag == null) return;
		if (!value.equals(this.editingTag.toString())) {
			try {
				this.editingTag = TagParser.parseTag(value);
				this.setEditorLocked(false);
				this.onTagEdited(this.editingTag.copy());
				this.parseError = null;
			} catch (CommandSyntaxException e) {
				this.parseError = e.getMessage();
				this.setEditorLocked(true);
			}
		} else {
			this.parseError = null;
			if (this.editorLocked) this.setEditorLocked(false);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.internalBoxX = this.leftPos + 23;
		this.internalBoxY = this.topPos + 26;

		if (!this.initialized) {
			this.tagBox = new ListenerEditBox(this.font, 0, 0, 129, 19, this.getTitle(), this::parseTagboxInput, ListenerEditBox.EDITBOX_BORDER_SPRITE);
			this.tagBox.setMaxLength(32000);
		}
		this.tagBox.setPosition(this.leftPos + 23, this.topPos + 5);
		this.addRenderableWidget(this.tagBox);

		if (!this.initialized) {
			this.addButton = new SpriteImageButton(0, 0, 16, 16, BUTTONS_SPRITE, b -> {
				TagEditingOverlay addOverlay = this.currentEnteredTag.getTagAddOverlay();
				if (addOverlay != null) {
					this.setOverlay(addOverlay);
				}
			}, null, false, 0, 0, BUTTON_SPRITE_LENGTH, BUTTON_SPRITE_HEIGHT);
		}
		this.addButton.setPosition(this.leftPos + 177, this.topPos + 6);
		this.addRenderableWidget(this.addButton);

		if (!this.initialized) {
			Component text = Component.translatable("blockomorph.gui.nbtEditor.restore");
			int textWidth = this.font.width(text);
			this.restoreButton = Button.builder(text, b -> {
				this.tagBox.setValue(this.editingTag.toString());
				this.setEditorLocked(false);
				this.parseError = null;
			}).size(textWidth + 6, 20).build();
			this.restoreButton.visible = false;
			this.addRenderableWidget(this.restoreButton);
		}
		this.restoreButton.setPosition(this.internalBoxX + this.internalBoxLength / 2 - this.restoreButton.getWidth() / 2, this.internalBoxY + 112);

		if (!this.initialized) {
			this.pathExit = Button.builder(Component.literal("<--"), button -> {
				this.enterInTag(this.path.back(), false);
			}).size(22, 9).build();
			this.pathExit.visible = false;
		}
		this.pathExit.setPosition(this.internalBoxX + 146, this.internalBoxY + 4);
		this.addRenderableWidget(this.pathExit);

		if (this.initialized) {
			this.reinitDeleteButtons();
		}
		if (this.overlay != null) {
			this.overlay.init(this.width, this.height, this::setOverlay);
		}
		this.initialized = true;
	}

	public void enterInTag(NbtPath path, boolean intr) {
		this.path = path;
		this.initList(intr);
	}

	@Nullable
	private TagRenderer<?> getTagByPath() {
		Tag root = this.editingTag;
		String previousName = "root";
		for (String tagName : this.path.constructPath(new ArrayList<>())) {
			TagRenderer<?> renderer = TagTypes.getRendererForTag(previousName, root, new TagRendererContext<>(this.provider(), this.onEntering, this.onTagEdited, this.onTagReceived, this::softEnteredTagRebuild));
			if (renderer != null) {
				Tag child = renderer.tryWalk(tagName);
				if (child != null) {
					root = child;
					previousName = tagName;
					continue;
				}
			}
			return null;
		}
		return TagTypes.getRendererForTag(previousName, root, new TagRendererContext<>(this.provider(), this.onEntering, this.onTagEdited, this.onTagReceived, this::softEnteredTagRebuild));
	}

	private void initList(boolean interpretationEnd) {
		TagRenderer<?> rootRenderer = this.getTagByPath();
		if (rootRenderer == null)
			throw new IllegalArgumentException("Illegal path: " + this.path + " for tag: " + this.editingTag);
		if (interpretationEnd)
			rootRenderer = Objects.requireNonNullElse(rootRenderer.getInterpretationRenderer(null), rootRenderer);
		if (rootRenderer.canEnterInTag()) {
			this.currentEnteredTag = rootRenderer;
			List<RenderableTag<?>> renderableTags = new ArrayList<>();
			for (TagRenderer<?> renderer : rootRenderer.getEnteringTags()) {
				renderableTags.add(new RenderableTag<>(renderer));
			}
			this.scrollerManager.setMainList(renderableTags.stream().filter(RenderableTag::isValid).toList());
			this.scrollerManager.setScrollOffset(0f);
			this.scrollerManager.refreshList();
			this.pathExit.visible = !this.path.isEmpty();
			return;
		}
		throw new IllegalArgumentException("Tag: " + rootRenderer.getTag().getType().getName() + " no enterable!");
	}

	private void softEnteredTagRebuild(HashMap<String, String> mappings) {
		List<String> nonInterpretation = Objects.requireNonNull(this.scrollerManager.getMainList()).stream().map(renderableTag -> {
			if (renderableTag.interpretationDisabled) {
				return mappings.get(renderableTag.mainRenderer.getName());
			}
			return null;
		}).filter(Objects::nonNull).toList();

		List<RenderableTag<?>> renderableTags = new ArrayList<>();
		for (TagRenderer<?> renderer : this.currentEnteredTag.getEnteringTags()) {
			RenderableTag<?> tag = new RenderableTag<>(renderer);
			renderableTags.add(tag);
			if (nonInterpretation.contains(tag.mainRenderer.getName())) {
				tag.disableInterpretation.run();
			}
		}

		this.scrollerManager.setMainList(renderableTags.stream().filter(RenderableTag::isValid).toList());
		if (this.scrollerManager.getMainList().size() <= MAX_PLATES_COUNT) {
			this.scrollerManager.setScrollOffset(0f);
		}
		this.scrollerManager.refreshList();
	}

	private void reinitDeleteButtons() {
		this.deleteButtons.clear();
		int i = 0;
		for (RenderableTag<?> tag : this.renderables) {
			this.deleteButtons.add(new SpriteImageButton(this.leftPos + 5, this.topPos + 42 + i * 20, 16, 16, BUTTONS_SPRITE, b -> {
				String name = tag.mainRenderer.getName();
				this.currentEnteredTag.deleteTag(name);
			}, null, false, 16, 0, BUTTON_SPRITE_LENGTH, BUTTON_SPRITE_HEIGHT));
			i++;
		}
	}

	private HolderGetter.Provider provider() {
		return GuiUtils.MC.level != null ? GuiUtils.MC.level.registryAccess().asGetterLookup() : RegistryAccess.EMPTY.asGetterLookup();
	}

	private class RenderableTag<T extends Tag> {
		TagRenderer<T> mainRenderer;
		boolean interpretationDisabled;
		Runnable disableInterpretation;

		private RenderableTag(TagRenderer<T> renderer) {
			this.mainRenderer = renderer;
			if (this.mainRenderer != null) {
				this.disableInterpretation = () -> {
					renderer.forceTagChange(this.mainRenderer.getTag());
					this.mainRenderer = renderer;
					this.interpretationDisabled = true;
				};
				TagRenderer<T> additionalRenderer = renderer.getInterpretationRenderer(this.disableInterpretation);
				if (additionalRenderer != null) {
					this.mainRenderer = additionalRenderer;
				}
			}
		}

		public boolean isValid() {
			return this.mainRenderer != null;
		}

		public void render(GuiUtils gui, int yOffset) {
			this.mainRenderer.getBox().setPosition(NbtEditorScreen.this.internalBoxX + 10, NbtEditorScreen.this.internalBoxY + 14 + yOffset);
			this.mainRenderer.renderTag(gui);
		}

		@Nullable
		public Component getTooltip(GuiUtils gui) {
			return this.mainRenderer.getTooltip(gui);
		}

		public boolean mouseClicked(double mouseX, double mouseY) {
			return this.mainRenderer.mouseClicked(mouseX, mouseY);
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double yOffsetWheel) {
			return this.mainRenderer.mouseScrolled(mouseX, mouseY, yOffsetWheel);
		}

		public boolean keyPressed(int key, int scancode, int mods) {
			return this.mainRenderer.keyPressed(key, scancode, mods);
		}

		public boolean charTyped(char character, int mods) {
			return this.mainRenderer.charTyped(character, mods);
		}

	}

}
