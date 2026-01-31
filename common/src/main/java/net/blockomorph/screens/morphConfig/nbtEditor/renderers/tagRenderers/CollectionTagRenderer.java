package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers;

import net.blockomorph.screens.morphConfig.nbtEditor.TagTypes;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagAddingOverlay;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive.NumericTagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class CollectionTagRenderer<LIST extends CollectionTag> extends TagRenderer<LIST> {
	private static final int BUTTON_SIZE = 16;
	private final TagType<?>[] allowedTypes;
	private static final List<Component> HINT = Stream.of(Component.translatable("blockomorph.gui.nbtEditor.addOverlay.hint.collectionTag").getString().split("\n")).map(word -> {
		return Component.translationArg(Component.literal(word));
	}).toList();
	private boolean hovered;
	private final int color;

	protected CollectionTagRenderer(String tagName, LIST tag, TagRendererContext<LIST> ctx, int color, TagType<?>... allowedTypes) {
		super(tagName, tag, ctx);
		this.color = color;
		this.allowedTypes = allowedTypes;
	}

	@Override
	public void renderPlate(GuiUtils gui, int number) {
		gui.fill(this.box.getX(), this.box.getY(), this.box.getX() + this.box.getWidth(), this.box.getY() + this.box.getHeight(), this.color);
		super.renderPlate(gui, number);
	}

	@Override
	public void render(GuiUtils gui) {
		int x = this.box.getX() + (this.hasName() ? MAX_NAME_WIDTH + 8 : this.box.getWidth() - 3 - BUTTON_SIZE);
		int y = this.box.getY() + 2;
		this.hovered = GuiUtils.isMouseOver(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, gui.getMouseX(), gui.getMouseY());
		gui.blit(TAGS_SPRITE, x, y, PLATE_LENGTH + BUTTON_SIZE, this.hovered ? BUTTON_SIZE : 0, BUTTON_SIZE, BUTTON_SIZE, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
	}

	@Override
	protected void renderLine(GuiUtils gui, int number) {
		this.renderDynamicColorLine(gui);
	}

	@Override
	public boolean canEnterInTag() {
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (this.hovered) {
			this.enterInTag();
			GuiUtils.playClickSound();
			return true;
		}
		return false;
	}

	@Override
	protected Integer getPlateNumber() {
		return 3;
	}

	@Override
	public int getFrameColor() {
		return this.color;
	}

	@Override
	public List<TagRenderer<?>> getEnteringTags() {
		List<TagRenderer<?>> renderers = new ArrayList<>();
		int i = 1;
		for (Tag tag : this.getTag()) {
			if (tag == null) continue;
			final int index = i;
			TagRenderer<?> renderer = TagTypes.getRendererForTag(index + "", tag, this.tagRendererContext.withTagUpdateListener(newTag -> {
				this.getTag().setTag(index - 1, newTag);
			}));
			//if (renderer != null) renderer.setNameVisibility(false); It's better not to turn it off for better orientation.
			renderers.add(renderer);
			i++;
		}
		return renderers;
	}

	@Override
	public Tag tryWalk(String nameElementInThisTag) {
		try {
			int index = Integer.parseInt(nameElementInThisTag) - 1;
			return this.getTag().get(index);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public TagEditingOverlay getTagAddOverlay() {
		return new TagAddingOverlay<>(name -> {
			if (name.isEmpty()) return true;
			try {
				int index = Integer.parseInt(name) - 1;
				if (index >= 0 && index < this.getTag().size()) {
					return true;
				}
			} catch (NumberFormatException e) {
				return false;
			}
			return false;
		}, (name, tag) -> {
			int index = name.isEmpty() ? this.getTag().size() : Integer.parseInt(name) - 1;
			this.signalChange(() -> {
				this.getTag().addTag(index, tag);
			}, false, index + 1);
		}, this::getAddOverlayHint, HINT, true, this.allowedTypes);
	}

	private String getAddOverlayHint() {
		if (this.getTag().isEmpty()) {
			return "*Adding to end*";
		} else if (this.getTag().size() == 1) {
			return "1";
		} else {
			return "1-" + (this.getTag().size());
		}
	}

	@Override
	public void deleteTag(String name) {
		try {
			int index = Integer.parseInt(name);
			if (index - 1 < this.getTag().size() && index - 1 >= 0) {
				this.signalChange(() -> {
					this.getTag().remove(index - 1);
				}, true, index);
			}
		} catch (Exception ignored) {
		}
	}

	private void signalChange(Runnable action, boolean delete, int userFormatIndex) {
		List<Integer> numbersOld = this.getNumberList();
		action.run();
		HashMap<String, String> map = new HashMap<>();
		for (int number : numbersOld) {
			if (number != userFormatIndex) {
				map.put(number + "", (number > userFormatIndex ? number + (delete ? -1 : 1) : number) + "");
			}
		}
		this.tagRendererContext.onSoftRebuildRequested().accept(map);
		this.tagRendererContext.onMainTagEdited().run();
	}

	private List<Integer> getNumberList() {
		List<Integer> numbers = new ArrayList<>();
		for (int i = 0; i < this.getTag().size(); i++) {
			numbers.add(i + 1);
		}
		return numbers;
	}


	public static final class ListTagRenderer extends CollectionTagRenderer<ListTag> {
		public ListTagRenderer(String tagName, ListTag tag, TagRendererContext<ListTag> ctx) {
			super(tagName, tag, ctx, ARGB.color(255, 245, 172, 47), TagTypes.getRegisteredTags());
		}
	}

	public static final class IntArrayTagRenderer extends CollectionTagRenderer<IntArrayTag> {
		public IntArrayTagRenderer(String tagName, IntArrayTag tag, TagRendererContext<IntArrayTag> ctx) {
			super(tagName, tag, ctx, NumericTagRenderer.INT_COLOR, IntTag.TYPE);
		}
	}

	public static final class LongArrayTagRenderer extends CollectionTagRenderer<LongArrayTag> {
		public LongArrayTagRenderer(String tagName, LongArrayTag tag, TagRendererContext<LongArrayTag> ctx) {
			super(tagName, tag, ctx, NumericTagRenderer.LONG_COLOR, LongTag.TYPE);
		}
	}

	public static final class ByteArrayTagRenderer extends CollectionTagRenderer<ByteArrayTag> {
		public ByteArrayTagRenderer(String tagName, ByteArrayTag tag, TagRendererContext<ByteArrayTag> ctx) {
			super(tagName, tag, ctx, NumericTagRenderer.BYTE_COLOR, ByteTag.TYPE);
		}
	}
}
