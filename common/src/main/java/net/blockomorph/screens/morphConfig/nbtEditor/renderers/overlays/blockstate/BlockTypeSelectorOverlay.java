package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.blockstate;

import net.blockomorph.screens.morph.AbstractMorphScreen;
import net.blockomorph.screens.morph.tabs.CreativeModeBlockTab;
import net.blockomorph.screens.morph.tabs.SavedBlocksTab;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.ScreenAdapterOverlay;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.SavedBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

import static net.blockomorph.screens.morph.tabs.TabManager.BLOCK_FRAME_SIZE;

public class BlockTypeSelectorOverlay extends ScreenAdapterOverlay<AbstractMorphScreen> {
	private static final ResourceLocation SELECTOR_SPRITE = GuiUtils.res("textures/screens/selector.png");

	public BlockTypeSelectorOverlay(Consumer<BlockState> onSelected) {
		super(new AbstractMorphScreen() {{
				this.tabManager.changeSpecialTabVisibility(SavedBlocksTab.INSTANCE, false);
			}

			@Override
			protected void initAdditional(Consumer<AbstractWidget> action) {
			}

			@Override
			protected void renderFrame(SavedBlock block, int x, int y) {
				if (GuiUtils.isMouseOver(x, y, x + BLOCK_FRAME_SIZE - 1, y + BLOCK_FRAME_SIZE, this.gui.getMouseX(), this.gui.getMouseY())) {
					this.gui.blitMonoImage(SELECTOR_SPRITE, x, y, BLOCK_FRAME_SIZE, BLOCK_FRAME_SIZE);
				}
			}

			@Override
			protected SoundInstance onClickOnBlock(SavedBlock block, int number, CreativeModeBlockTab selectedTab, int page) {
				onSelected.accept(block.getState());
				return GuiUtils.getClickSound();
			}

			@Override
			public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
				super.render(guiGraphics, mouseX, mouseY, tick);
				Component text = Component.translatable("blockomorph.gui.stateSelectorOverlay.selectBlockType");
				this.gui.drawCenteredString(text, this.width / 2, this.topPos - (this.tabManager.hasSearchBar() ? 30 : 20), -1, true);
			}
		});
	}
}
