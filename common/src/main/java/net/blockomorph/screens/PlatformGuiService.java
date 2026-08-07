package net.blockomorph.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ServiceLoader;

public interface PlatformGuiService {
	PlatformGuiService INSTANCE = ServiceLoader.load(PlatformGuiService.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load GUI platform-depended utils, mod cannot run!"));

	@Nullable ScreenRectangle scissorsPeek(GuiGraphicsExtractor guiGraphics);
	void submitCustomPipRenderState(GuiGraphicsExtractor guiGraphics, PictureInPictureRenderState renderState);

	boolean hasSearchBarInTab(CreativeModeTab tab);
	void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters,
								 ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator contentGenerator);
	void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, ItemsOutputAccess output, CreativeModeTab.DisplayItemsGenerator vanillaGenerator);
	interface ItemsOutputAccess {
		void accept(ItemStack item, CreativeModeTab.TabVisibility tabVisibility);
	}
}
