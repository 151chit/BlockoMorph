package net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers;

import net.blockomorph.screens.morphConfig.nbtEditor.TagTypes;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers.AbstractInterpritationTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers.BlockStateTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagAddingOverlay;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ARGB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CompoundTagRenderer extends TagRenderer<CompoundTag> {
	private static final int BUTTON_SIZE = 16;
	private boolean hovered;

	public CompoundTagRenderer(String tagName, CompoundTag tag, TagRendererContext<CompoundTag> ctx) {
		super(tagName, tag, ctx);
	}

	@Override
	public void render(GuiUtils gui) {
		int x = this.box.getX() + (this.hasName() ? MAX_NAME_WIDTH + 8 : this.box.getWidth() - 3 - BUTTON_SIZE);
		int y = this.box.getY() + 2;
		this.hovered = GuiUtils.isMouseOver(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, gui.getMouseX(), gui.getMouseY());
		gui.blit(TAGS_SPRITE, x, y, PLATE_LENGTH, this.hovered ? BUTTON_SIZE : 0, BUTTON_SIZE, BUTTON_SIZE, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
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
	public int getFrameColor() {
		return ARGB.color(255, 210, 166, 132);
	}

	@Override
	public List<TagRenderer<?>> getEnteringTags() {
		List<TagRenderer<?>> renderers = new ArrayList<>();
		for (String key : this.getTag().getAllKeys()) {
			Tag tag = this.getTag().get(key);
			if (tag == null) throw new NullPointerException();
			renderers.add(TagTypes.getRendererForTag(key, tag, this.tagRendererContext.withTagUpdateListener(newTag -> {
				this.getTag().put(key, newTag);
			})));
		}
		return renderers;
	}

	@Override
	public Tag tryWalk(String nameElementInThisTag) {
		return this.getTag().get(nameElementInThisTag);
	}

	@Override
	protected Integer getPlateNumber() {
		return 2;
	}

	@Override
	public TagEditingOverlay getTagAddOverlay() {
		return new TagAddingOverlay<>(Objects::nonNull, (name, tag) -> {
			this.getTag().put(name, tag);
			this.signalChange();
		}, () -> "Tag name", null, true, TagTypes.getRegisteredTags());
	}

	@Override
	public void deleteTag(String name) {
		this.getTag().remove(name);
		this.signalChange();
	}

	private void signalChange() {
		HashMap<String, String> map = new HashMap<>();
		for (String tagName : this.getTag().getAllKeys()) {
			map.put(tagName, tagName);
		}
		this.tagRendererContext.onSoftRebuildRequested().accept(map);
		this.tagRendererContext.onMainTagEdited().run();
	}

	@Override
	public AbstractInterpritationTagRenderer<CompoundTag> getInterpretationRenderer(Runnable onInterpretationBrake) {
		CompoundTag self = this.getTag();
		if (self.contains("Name")) {
			if (self.contains("Properties") && self.size() == 2) {
				return this.getBlockStateRenderer(onInterpretationBrake);
			} else {
				if (MorphUtils.blockPredicate().test(self.getString("Name")) && self.size() == 1) {
					return this.getBlockStateRenderer(onInterpretationBrake);
				}
			}
		}
		return null;
	}

	private BlockStateTagRenderer getBlockStateRenderer(Runnable onInterpretationBrake) {
		return new BlockStateTagRenderer(this, this.tagRendererContext.forInterpretation(onInterpretationBrake));
	}
}
