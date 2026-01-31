package net.blockomorph.screens.morphConfig.nbtEditor.renderers.interpritationTagRenderers;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.blockstate.BlockStateSelectorOverlay;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.CompoundTagRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateTagRenderer extends AbstractInterpritationTagRenderer<CompoundTag> {
	private static final int BUTTON_SIZE = 16;
	private BlockState state;
	private BlockEntity tempBE;
	private boolean hovered;

	public BlockStateTagRenderer(CompoundTagRenderer parent, TagRendererContext<CompoundTag> ctx) {
		super(parent, ctx);
		this.state = NbtUtils.readBlockState(ctx.provider().lookupOrThrow(Registries.BLOCK), parent.getTag());
		this.setTempBE();
	}

	private void setTempBE() {
		this.tempBE = this.state.getBlock() instanceof EntityBlock ent ? ent.newBlockEntity(GuiUtils.AIR, this.state) : null;
		if (this.tempBE != null) {
			this.tempBE.setLevel(GuiUtils.MC.level);
		}
	}

	@Override
	public void renderMain(GuiUtils gui) {
		int x = this.box.getX() + this.box.getWidth() - 29 - BUTTON_SIZE;
		int y = this.box.getY() + 2;
		this.hovered = GuiUtils.isMouseOver(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, gui.getMouseX(), gui.getMouseY());
		if (this.hovered)
			gui.blit(TAGS_SPRITE, x, y, PLATE_LENGTH, 46, BUTTON_SIZE, BUTTON_SIZE, PLATE_SPRITE_LENGTH, PLATE_SPRITE_HEIGTH);
		gui.renderBlockInGui(this.state, this.tempBE, x + 13.75f, y + BUTTON_SIZE - 4.5f, 8f, null);
		gui.renderAdditionalOnBlock(this.state, x, y, 14f);
	}

	@Override
	public boolean mouseClick(double mouseX, double mouseY) {
		if (this.hovered) {
			this.tagRendererContext.onTagEditingRequested().accept(new BlockStateSelectorOverlay(this.state, this::changeBlockState));
			GuiUtils.playClickSound();
			return true;
		}
		return false;
	}

	@Override
	public Component getTooltip(GuiUtils gui) {
		if (this.hovered) {
			return this.state.getBlock().getName();
		}
		return super.getTooltip(gui);
	}

	private void changeBlockState(BlockState state) {
		this.state = state;
		this.setTempBE();
		CompoundTag tag = NbtUtils.writeBlockState(state);
		this.changeThis(tag);
	}

	@Override
	protected Integer getPlateNumber() {
		return 5;
	}
}
