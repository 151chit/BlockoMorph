package net.blockomorph.platformUtilsImpl;

import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.impl.creativetab.CreativeModeTabEventsImpl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class FabricClientUtils implements ClientPlatformUtils {
	@Override
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, OutPutBm output, CreativeModeTab.DisplayItemsGenerator orig) {
		List<ItemStack> mutableDisplayStacks = new LinkedList<>();
		List<ItemStack> mutableSearchTabStacks = new LinkedList<>();
		orig.accept(parameters, (stack, tabVisibility) -> {
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
	public void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, OutPutBm outPutBm, CreativeModeTab.DisplayItemsGenerator content) {
		content.accept(parameters, outPutBm::accept);
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.getType() == CreativeModeTab.Type.SEARCH;
	}

	@Override
	public void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state) {
		//do nothing
	}

	@Override
	public boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine) {
		return state.shouldSpawnTerrainParticles();
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
