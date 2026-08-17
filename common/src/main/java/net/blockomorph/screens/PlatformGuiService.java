package net.blockomorph.screens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ServiceLoader;

public interface PlatformGuiService {
	PlatformGuiService INSTANCE = ServiceLoader.load(PlatformGuiService.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load GUI platform-depended utils, mod cannot run!"));

	boolean hasSearchBarInTab(CreativeModeTab tab);
	void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters,
								 ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator contentGenerator);
	void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator vanillaGenerator);
	interface ItemsOutputAccess {
		void accept(ItemStack item, CreativeModeTab.TabVisibility tabVisibility);
	}
}
