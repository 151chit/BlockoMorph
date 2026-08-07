package net.blockomorph.platformImpl;

import net.blockomorph.screens.PlatformGuiService;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.impl.creativetab.CreativeModeTabEventsImpl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

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

		FabricCreativeModeTabOutput entries = new FabricCreativeModeTabOutput(parameters, mutableDisplayStacks, mutableSearchTabStacks);
		Event<CreativeModeTabEvents.ModifyOutput> modifyEntriesEvent = CreativeModeTabEventsImpl.getModifyOutputEvent(key);
		if (modifyEntriesEvent != null) {
			modifyEntriesEvent.invoker().modifyOutput(entries);
		}

		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.invoker().modifyOutput(tab, entries);

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

	@Override @Nullable
	public ScreenRectangle scissorsPeek(GuiGraphicsExtractor guiGraphics) {
		return guiGraphics.scissorStack.peek();
	}

	@Override
	public void submitCustomPipRenderState(GuiGraphicsExtractor guiGraphics, PictureInPictureRenderState renderState) {
		guiGraphics.guiRenderState.addPicturesInPictureState(renderState);
	}
}
