package net.blockomorph.screens.morph.tabs;

import net.minecraft.world.item.CreativeModeTabs;

public class SearchTab extends CreativeModeBlockTab {
	public static final SearchTab INSTANCE = new SearchTab();
	private SearchTab() {
		super(CreativeModeTabs.SEARCH, CreativeModeTabs.searchTab());
	}

	@Override
	public boolean hasSearchBar() {
		return true;
	}
}
