package net.blockomorph.platformUtilsImpl;

import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

public class NeoClientUtils implements ClientPlatformUtils {
	@Override
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, OutPutBm output, CreativeModeTab.DisplayItemsGenerator orig) {
		EventHooks.onCreativeModeTabBuildContents(tab, orig, parameters, output::accept);
	}

	@Override
	public void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, OutPutBm outPutBm, CreativeModeTab.DisplayItemsGenerator content) {
		content.accept(parameters, outPutBm::accept);
	}

	@Override
	public FogLiquidModifier.LiquidFogData calculateData(Level lv, BlockInPlayer2 block) {
		return ClientPlatformUtils.super.calculateData(lv, block);//TODO!!!!!!!!!
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.hasSearchBar();
	}

	@Override
	public void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state) {
		particle.updateSprite(state, keyPos);
	}

	@Override
	public boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine) {
		return !IClientBlockExtensions.of(state).addDestroyEffects(state, level, keyPos, engine);
	}

	@Override @Nullable
	public ScreenRectangle scissorsPeek(GuiGraphicsExtractor GuiGraphicsExtractor) {
		return GuiGraphicsExtractor.peekScissorStack();
	}

	@Override
	public void submitCustomPipRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, PictureInPictureRenderState renderState) {
		GuiGraphicsExtractor.submitPictureInPictureRenderState(renderState);
	}
}
