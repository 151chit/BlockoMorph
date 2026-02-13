package net.blockomorph.platformUtilsImpl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.impl.itemgroup.ItemGroupEventsImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FabricClientUtils implements ClientPlatformUtils {
	private static final Supplier<Minecraft> MC = () -> GuiUtils.MC;

	@Override
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output, CreativeModeTab.DisplayItemsGenerator orig) {
		List<ItemStack> mutableDisplayStacks = new ArrayList<>();
		List<ItemStack> mutableSearchTabStacks = new ArrayList<>();
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
	public @Nullable TextureAtlasSprite[] getPlatformFluidSprite(BlockAndTintGetter lv, BlockState blockState, BlockPos pos) {
		FluidState fluidState = blockState.getFluidState();
		FluidRenderHandler info = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());
		if (info != null) {
			return info.getFluidSprites(lv, pos, fluidState);
		}
		return null;
	}

	@Override
	public Integer getPlatformFluidTint(BlockAndTintGetter lv, BlockState blockState, BlockPos pos) {
		FluidState fluidState = blockState.getFluidState();
		FluidRenderHandler info = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());
		if (info != null) {
			return info.getFluidColor(lv, pos, fluidState);
		}
		return null;
	}

	@Override
	public void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {
		RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
		if (blockState.getRenderShape() == RenderShape.MODEL) {
			List<BlockModelPart> modelParts = MC.get().getBlockRenderer().getBlockModel(blockState).collectParts(random);
			var renderType = ItemBlockRenderTypes.getMovingBlockRenderType(blockState);
			ClientLevelAccessor acc = ClientLevelAccessor.of(MC.get().level);
			acc.setSpecialRenderingMode(true);
			MC.get().getBlockRenderer().getModelRenderer().tesselateBlock(MC.get().level, modelParts, blockState, zeroOrFake, stack, bufferSource.getBuffer(renderType), false, OverlayTexture.NO_OVERLAY);
			acc.setSpecialRenderingMode(false);
		}
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
	public ScreenRectangle scissorsPeek(GuiGraphics guiGraphics) {
		return guiGraphics.scissorStack.peek();
	}

	@Override
	public void submitCustomPipRenderState(GuiGraphics guiGraphics, PictureInPictureRenderState renderState) {
		guiGraphics.guiRenderState.submitPicturesInPictureState(renderState);
	}
}
