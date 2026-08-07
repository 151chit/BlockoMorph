package net.blockomorph.platformImpl;

import net.blockomorph.screens.PlatformGuiService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public class NeoGuiService implements PlatformGuiService {

	@Override @SuppressWarnings("UnstableApiUsage")
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator contentGenerator) {
		EventHooks.onCreativeModeTabBuildContents(tab, contentGenerator, parameters, output::accept);
	}

	@Override
	public void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator vanillaGenerator) {
		vanillaGenerator.accept(parameters, output::accept);
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.hasSearchBar();
	}

	@Override @Nullable
	public ScreenRectangle scissorsPeek(GuiGraphicsExtractor guiGraphics) {
		return guiGraphics.peekScissorStack();
	}

	@Override
	public void submitCustomPipRenderState(GuiGraphicsExtractor guiGraphics, PictureInPictureRenderState renderState) {
		guiGraphics.submitPictureInPictureRenderState(renderState);
	}
}
