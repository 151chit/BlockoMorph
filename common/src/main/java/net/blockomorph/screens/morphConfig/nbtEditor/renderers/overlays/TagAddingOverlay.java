package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays;

import net.blockomorph.screens.morphConfig.nbtEditor.NbtEditorScreen;
import net.blockomorph.screens.morphConfig.nbtEditor.TagTypes;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.TagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ObjectListRenderer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TagAddingOverlay<T extends Tag> extends TagEditingOverlay {
	private static final ResourceLocation MENU = GuiUtils.res("textures/screens/tag_adding_screen.png");
	private int tagBoxX;
	private int tagBoxY;
	private final ObjectListRenderer<TagTypes, TagType<? extends T>> selector;
	private final BiConsumer<String, T> onTagCreated;
	private CachedType<? extends T> currentType;
	private final TagType<? extends T> first;
	@Nullable private final Supplier<String> nameSuggestion;
	@Nullable private final List<Component> hint;
	private int leftPos;
	private int topPos;
	private final boolean listDisabled;
	private final Predicate<String> filter;
	private EditBox name;

	@SafeVarargs //TODO - Add interpretation patterns
	public TagAddingOverlay(Predicate<String> nameFilter, BiConsumer<String, T> onTagCreated, @Nullable Supplier<String> nameSuggestion, @Nullable List<Component> hint, boolean needInterpritation, TagType<? extends T>... types) {
		super(159, 59);
		List<TagType<?>> registered = List.of(TagTypes.getRegisteredTags());
		List<TagType<? extends T>> filtered = Stream.of(types).filter(type -> {
			if (registered.contains(type)) return true;
			MorphUtils.LOGGER.error("Tag " + type.getName() + " not registered and not be added!");
			return false;
		}).toList();
		if (filtered.isEmpty()) throw new IllegalArgumentException("Types array is empty!");
		this.selector = new ObjectListRenderer<>(-1, -256, 7, (tagTypes, tagType) -> {
			return tagType.getPrettyName().replace("TAG_", "").replace("_", " ");
		}, tagTypes -> {
			return filtered;
		});
		this.onTagCreated = onTagCreated;
		this.listDisabled = filtered.size() == 1;
		this.filter = nameFilter;
		this.first = filtered.getFirst();
		this.nameSuggestion = nameSuggestion;
		this.hint = hint;
	}

	private <TAG extends T> void setCurrentType(TagType<TAG> type) {
		Supplier<TAG> creator = TagTypes.createDefaultTag(type);
		this.currentType = new CachedType<>(type, creator, TagTypes.getRendererForTag(null, Objects.requireNonNull(creator).get(), null));
		this.currentType.renderer.getBox().setPosition(this.tagBoxX, this.tagBoxY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (type == 0) {
			if (!this.listDisabled) {
				int x = this.tagBoxX;
				int y = this.tagBoxY;
				if (this.selector.mouseClicked(mouseX, mouseY)) {
					return true;
				} else if (GuiUtils.isMouseOver(x, y, x + 54, y + 20, mouseX, mouseY)) {
					this.selector.drop(null, tagType -> {
						GuiUtils.playClickSound();
						this.setCurrentType(tagType);
					});
					GuiUtils.playClickSound();
					return true;
				}
				this.selector.close();
			}
			return super.mouseClicked(mouseX, mouseY, 0);
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xWheelOffset, double yWheelOffset) {
		return this.selector.mouseScrolled(mouseX, mouseY, yWheelOffset);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (key == 257) {
			this.acceptTag();
			GuiUtils.playClickSound();
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	@Override
	protected void renderInGui(GuiUtils gui) {
		int x = this.tagBoxX + 6;
		int y = this.tagBoxY + 6;
		this.selector.renderName(gui, x, y, 38, null, this.currentType.type, -1);
		this.selector.render(gui, x, y, -1, 10);
		if (this.nameSuggestion != null && this.name.getValue().isEmpty() && !this.name.isHovered())
			gui.drawString(Component.literal(this.nameSuggestion.get()), this.name.getX() + 4, this.name.getY() + (this.name.getHeight() - 8) / 2, -8355712, true);
		if (this.hint != null && this.name.isHovered()) {
			int height = 3 + 3 + (this.hint.size() * gui.getFont().lineHeight) + 9 * 2;
			gui.renderTooltip(this.hint, this.name.getX() - 12, this.name.getY() - height + 12);
		}
	}

	public void acceptTag() {
		T tag = this.currentType.creator.get();
		this.onTagCreated.accept(this.name.getValue(), tag);
		this.onChange.accept(null);
	}

	@Override
	protected void renderBackground(GuiUtils gui) {
		TagRenderer<?> renderer = this.currentType.renderer;
		gui.blitMonoImage(MENU, this.leftPos, this.topPos, this.imageLength, this.imageHeight);

		gui.enableScissors(this.tagBoxX, this.tagBoxY, this.tagBoxX + 50, this.tagBoxY + 20);
		renderer.renderPlateWithoutCtx(gui);
		gui.disableScissors();

		int x = this.tagBoxX + 50;
		gui.enableScissors(x, this.tagBoxY, x + 4, this.tagBoxY + 20);
		int plateX = x - (renderer.getBox().getWidth() - 4);
		int oldX = renderer.getBox().getX();
		renderer.getBox().setX(plateX);
		renderer.renderPlateWithoutCtx(gui);
		renderer.getBox().setX(oldX);
		gui.disableScissors();

		gui.blit(NbtEditorScreen.BUTTONS_SPRITE, this.tagBoxX + 4, this.tagBoxY + 4, 0, 32, 46, 11, NbtEditorScreen.BUTTON_SPRITE_LENGTH, NbtEditorScreen.BUTTON_SPRITE_HEIGHT);
	}

	@Override
	public void init(int width, int height, Consumer<TagEditingOverlay> onChange) {
		super.init(width, height, onChange);
		this.leftPos = (this.width - this.imageLength) / 2;
		this.topPos = (this.height - this.imageHeight) / 2;
		this.tagBoxX = this.leftPos + 99;
		this.tagBoxY = this.topPos + 7;

		if (this.currentType == null) this.setCurrentType(this.first);
		this.currentType.renderer.getBox().setPosition(this.tagBoxX, this.tagBoxY);

		String nameValue = this.name != null ? this.name.getValue() : "";
		this.name = new EditBox(GuiUtils.MC.font, this.leftPos + 7, this.topPos + 7, 89, 20, Component.literal("name"));
		this.name.setFilter(this.filter);
		this.addRenderableWidget(this.name);
		this.name.setValue(nameValue);

		Button done = new Button(this.leftPos + 7, this.topPos + 32, 146, 20, CommonComponents.GUI_DONE, b -> {
			this.acceptTag();
		}, Supplier::get) {
			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
				this.isHovered = this.isMouseOver(mouseX, mouseY) && !TagAddingOverlay.this.selector.isListFocused(mouseX, mouseY);
				super.renderWidget(guiGraphics, mouseX, mouseY, tick);
			}
		};
		this.addRenderableWidget(done);
		this.setFocused(this.name);
	}

	private record CachedType<T extends Tag>(TagType<T> type, Supplier<T> creator, TagRenderer<T> renderer) {
	}
}
