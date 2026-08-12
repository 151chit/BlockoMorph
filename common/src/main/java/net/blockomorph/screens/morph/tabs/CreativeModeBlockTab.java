package net.blockomorph.screens.morph.tabs;

import net.blockomorph.screens.PlatformGuiService;
import net.blockomorph.screens.morph.TabContentManager;
import net.blockomorph.utils.SavedBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public abstract class CreativeModeBlockTab {
	public final ResourceKey<CreativeModeTab> key;
	public final CreativeModeTab realTab;

	protected CreativeModeBlockTab(ResourceKey<CreativeModeTab> key, CreativeModeTab realTab) {
		this.key = key;
		this.realTab = realTab;
	}

	public Component getDisplayName() {
		return this.realTab.getDisplayName();
	}

	public ItemStack getIconItem() {
		return this.realTab.getIconItem();
	}

	public boolean showTitle() {
		return this.realTab.showTitle();
	}

	public boolean hasSearchBar() {
		return PlatformGuiService.INSTANCE.hasSearchBarInTab(this.realTab);
	}

	public boolean needShow() {
		return !this.getBlockSource().isEmpty();
	}

	public List<SavedBlock> getBlockSource() {
		List<SavedBlock> blocks = TabContentManager.getTabContents().get(this.key);
		if (blocks == null) return List.of();
		return blocks;
	}

	public void onPermissionChanged() {}

	public void onConfigChanged() {}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CreativeModeBlockTab tab) {
			return tab.key.equals(this.key);
		}
		return false;
	}
}
