package net.blockomorph.screens.morph.tabs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.Map;

public class ContentCreativeModeTab extends CreativeModeBlockTab {
	private static final Map<ResourceKey<CreativeModeTab>, ContentCreativeModeTab> TABS = new HashMap<>();
	protected ContentCreativeModeTab(ResourceKey<CreativeModeTab> key, CreativeModeTab realTab) {
		super(key, realTab);
	}

	public static ContentCreativeModeTab get(CreativeModeTab tab) {
		ResourceKey<CreativeModeTab> key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow(() -> {
			return new IllegalArgumentException("Not registered content tab in blockomorph GUI!");
		});
		if (TABS.containsKey(key)) {
			return TABS.get(key);
		} else {
			ContentCreativeModeTab contentTab = new ContentCreativeModeTab(key, tab);
			TABS.put(key, contentTab);
			return contentTab;
		}
	}

	public static void releaseCache() {
		TABS.clear();
	}
}
