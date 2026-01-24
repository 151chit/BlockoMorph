package net.blockomorph.screens.morph.tabs;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.SavedBlockManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.List;

public class SavedBlocksTab extends CreativeModeBlockTab {
	public static final SavedBlockManager CONTENT_MANAGER = new SavedBlockManager(MorphUtils.getGameDir());
	public static final SavedBlocksTab INSTANCE = new SavedBlocksTab();

	private SavedBlocksTab() {
		super(CreativeModeTabs.HOTBAR, BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.HOTBAR));
	}

	@Override
	public boolean hasSearchBar() {
		return true;
	}

	@Override
	public boolean needShow() {
		CONTENT_MANAGER.load();
		return !CONTENT_MANAGER.get().isEmpty();
	}

	@Override
	public List<SavedBlock> getBlockSource() {
		CONTENT_MANAGER.load();
		return CONTENT_MANAGER.get().values().stream().toList();
	}
}
