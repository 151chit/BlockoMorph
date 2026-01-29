package net.blockomorph.screens.morph.tabs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

public class OperatorTab extends CreativeModeBlockTab {
	public static final OperatorTab INSTANCE = new OperatorTab();

	private OperatorTab() {
		super(CreativeModeTabs.OP_BLOCKS, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.OP_BLOCKS));
	}
}
