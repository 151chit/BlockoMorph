package net.blockomorph.platformImpl;

import net.blockomorph.screens.PlatformGuiService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.EventHooks;

public class NeoGuiService implements PlatformGuiService {

	@Override @SuppressWarnings("UnstableApiUsage")
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator contentGenerator) {
		EventHooks.onCreativeModeTabBuildContents(tab, key, contentGenerator, parameters, output::accept);
	}

	@Override
	public void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator vanillaGenerator) {
		vanillaGenerator.accept(parameters, output::accept);
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.hasSearchBar();
	}
}
