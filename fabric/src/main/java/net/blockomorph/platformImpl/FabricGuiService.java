package net.blockomorph.platformImpl;

import net.blockomorph.screens.PlatformGuiService;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.impl.itemgroup.ItemGroupEventsImpl;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class FabricGuiService implements PlatformGuiService {

	@Override @SuppressWarnings("UnstableApiUsage")
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key,
										CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator contentGenerator) {
		List<ItemStack> mutableDisplayStacks = new LinkedList<>();
		List<ItemStack> mutableSearchTabStacks = new LinkedList<>();
		contentGenerator.accept(parameters, (stack, tabVisibility) -> {
			if (stack.getCount() != 1)
				throw new IllegalArgumentException("The stack count must be 1");

			boolean all = tabVisibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
			if (all || tabVisibility == CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
				mutableDisplayStacks.add(stack);
			}

			if (all || tabVisibility == CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
				mutableSearchTabStacks.add(stack);
			}
		});

		FabricItemGroupEntries fabricTabCollector = new FabricItemGroupEntries(parameters, mutableDisplayStacks, mutableSearchTabStacks);
		Event<ItemGroupEvents.ModifyEntries> modifyEntriesEvent = ItemGroupEventsImpl.getModifyEntriesEvent(key);
		if (modifyEntriesEvent != null) {
			modifyEntriesEvent.invoker().modifyEntries(fabricTabCollector);
		}
		ItemGroupEvents.MODIFY_ENTRIES_ALL.invoker().modifyEntries(tab, fabricTabCollector);

		mutableDisplayStacks.forEach(item -> output.accept(item, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
		mutableSearchTabStacks.forEach(item -> output.accept(item, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY));
	}

	@Override
	public void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator vanillaGenerator) {
		vanillaGenerator.accept(parameters, output::accept);
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.getType() == CreativeModeTab.Type.SEARCH;
	}
}
