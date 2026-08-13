package net.blockomorph.screens.morph;

import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.morph.tabs.CreativeModeBlockTab;
import net.blockomorph.screens.morph.tabs.TabManager;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ScrollerManager;
import net.blockomorph.utils.SavedBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public abstract class AbstractMorphScreen extends AbstractScreen implements ConfigSyncListener {
	private static final ResourceLocation SEARCH_BAR = GuiUtils.res("textures/screens/searchbar.png");
	protected final TabManager tabManager;

	protected AbstractMorphScreen() {
		super("morph_screen", null);
		this.tabManager = new TabManager(() -> this.leftPos, () -> this.topPos, this.imageHeight, this.imageLength);
	}

	protected abstract void initAdditional(Consumer<AbstractWidget> action);

	protected abstract void renderFrame(SavedBlock block, int x, int y);

	protected abstract SoundInstance onClickOnBlock(SavedBlock block, int number, CreativeModeBlockTab selectedTab, int page);

	protected void renderTooltipForBlock() {
		SavedBlock block = this.tabManager.getBlockAtPosition(gui.getMouseX(), gui.getMouseY());
		if (block != null) {
			Component name = block.getName() == null ? block.getState().getBlock().getName() : Component.literal(block.getName());
			gui.renderTooltip(name, gui.getMouseX(), gui.getMouseY());
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
		super.render(guiGraphics, mouseX, mouseY, tick);
		this.tabManager.renderTabs(this.gui);
		this.tabManager.renderBlocksInSlots(this.gui, this::renderFrame);
		this.renderTooltipForBlock();
		CreativeModeBlockTab tab = this.tabManager.getTabAtPosition(gui.getMouseX(), gui.getMouseY());
		if (tab != null) gui.renderTooltip(tab.getDisplayName(), gui.getMouseX(), gui.getMouseY());
	}

	@Override
	protected void renderMenu() {
		super.renderMenu();
		if (this.tabManager.hasSearchBar()) {
			gui.blitMonoImage(SEARCH_BAR, this.leftPos + 90, this.topPos - 19, 80, 23);
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int type) {
		if (type == 0) {
			if (this.tabManager.mouseClicked(x, y, this::onClickOnBlock)) {
				return true;
			}
		}
		return super.mouseClicked(x, y, type);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int type, double mouseXOffset, double mouseYOffset) {
		if (this.tabManager.scrollerManager.mouseDragged(mouseY)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, type, mouseXOffset, mouseYOffset);
	}

	@Override
	public boolean mouseReleased(double x, double y, int type) {
		if (type == 0) {
			this.tabManager.scrollerManager.disableScrollWork();
		}
		return super.mouseReleased(x, y, type);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double yScrolled) {
		if (this.tabManager.scrollerManager.mouseScrolled(yScrolled)) {
			return true;
		}
		return super.mouseScrolled(x, y, yScrolled);
	}

	@Override
	public boolean charTyped(char codePoint, int mods) {
		if (this.tabManager.getSearchBox().charTyped(codePoint, mods)) {
			return true;
		}
		return super.charTyped(codePoint, mods);
	}

	@Override
	public boolean keyPressed(int keyboardButton, int scanCode, int mods) {
		if (keyboardButton == 256) {
			this.onClose();
		} else {
			if (this.tabManager.getSearchBox().keyPressed(keyboardButton, scanCode, mods)) {
				return true;
			}
		}
		return super.keyPressed(keyboardButton, scanCode, mods);
	}

	@Override
	protected void init() {
		super.init();
		this.initAdditional(this::addRenderableWidget);
		this.tabManager.init(this::addRenderableWidget);
		TabContentManager.bakeTabs();
		this.tabManager.rebuildLists(true);
	}

	@Override
	public void onConfigSynced() {
		this.tabManager.forEachTab(CreativeModeBlockTab::onConfigChanged);
		this.tabManager.rebuildLists(false);
	}

	@Override
	public void onOperatorRightsChanged() {
		this.tabManager.forEachTab(CreativeModeBlockTab::onPermissionChanged);
		this.tabManager.rebuildLists(false);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		ScrollerManager<SavedBlock> manager = this.tabManager.scrollerManager;
		float scroll = manager.getScrollerOffset();
		String value = this.tabManager.getSearchBox().getValue();
		super.resize(minecraft, width, height);
		this.tabManager.getSearchBox().setValue(value);
		this.tabManager.searchBlocks(value);
		manager.setScrollOffset(scroll);
		manager.refreshList();
	}

	@FunctionalInterface
	public interface OnBlockClick {
		SoundInstance click(SavedBlock block, int number, CreativeModeBlockTab tab, int page);
	}

	@FunctionalInterface
	public interface OnRenderingFrame {
		void render(SavedBlock block, int x, int y);
	}
}
