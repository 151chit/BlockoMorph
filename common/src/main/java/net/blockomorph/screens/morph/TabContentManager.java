package net.blockomorph.screens.morph;

import net.blockomorph.screens.PlatformGuiService;
import net.blockomorph.screens.morph.tabs.ContentCreativeModeTab;
import net.blockomorph.screens.morph.tabs.SavedBlocksTab;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TabContentManager {
	private static Context CONTEXT;
	private static volatile Map<ResourceKey<CreativeModeTab>, List<SavedBlock>> TAB_CONTENTS = Map.of();
	private static List<Block> ALL_BLOCKS = List.of();

	public static List<Block> getAllBlocks() {
		return ALL_BLOCKS;
	}

	public static Map<ResourceKey<CreativeModeTab>, List<SavedBlock>> getTabContents() {//search, unsortable and content
		return TAB_CONTENTS;
	}

	public static Context getContext() {
		return CONTEXT;
	}

	public static void bakeTabs() {
		LocalPlayer player = GuiUtils.MC.player;
		if (player == null) return;
		Context currentCtx = new Context(player.connection.enabledFeatures(), player.level().registryAccess());
		if (CONTEXT == null || !CONTEXT.equals(currentCtx)) {
			CONTEXT = currentCtx;
			ContentCreativeModeTab.releaseCache();
			SavedBlocksTab.CONTENT_MANAGER.load();
			HashMap<ResourceKey<CreativeModeTab>, List<SavedBlock>> content = new HashMap<>();
			Set<Block> seen = new HashSet<>();
			formContentTab(seen, content, currentCtx);
			List<Block> allBlocks = BuiltInRegistries.BLOCK.stream().filter((block -> block.isEnabled(CONTEXT.flagSet()))).toList();
			ALL_BLOCKS = allBlocks;
			formSearchTab(allBlocks, content);
			formUnsortableTab(seen, allBlocks, content);
			TAB_CONTENTS = content;
		}
	}

	private static void formContentTab(Set<Block> seen, HashMap<ResourceKey<CreativeModeTab>, List<SavedBlock>> content, Context context) {
		CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(context.flagSet, true, context.holders);
		BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().forEach((entry) -> {
			ResourceKey<CreativeModeTab> key = entry.getKey();
			CreativeModeTab tab = entry.getValue();
			ArrayList<SavedBlock> blocks = new ArrayList<>();
			if (tab.getType() == CreativeModeTab.Type.CATEGORY && tab instanceof Accessors.CategoryTabAccessor acc) {
				preparePlatformDependEvent(tab, key, parameters, (itemStack, tabVisibility) -> {
					if (tabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
						if (itemStack.getItem() instanceof BlockItem blockItem) {
							Block block = blockItem.getBlock();
							if (block.isEnabled(context.flagSet())) {
								seen.add(block);
								blocks.add(new SavedBlock(prepareBlockStateTag(block.defaultBlockState(), itemStack), getTagForBlockEntity(itemStack), null));
							}
						}
					}
				}, acc.getItemsFormer$bm());
			}
			content.put(key, Collections.unmodifiableList(blocks));
		});
	}

	private static void formSearchTab(List<Block> allBlocks, HashMap<ResourceKey<CreativeModeTab>, List<SavedBlock>> content) {
		content.put(CreativeModeTabs.SEARCH, allBlocks.stream().map(block ->
				new SavedBlock(block.defaultBlockState(), null, null)
		).toList());
	}

	private static void formUnsortableTab(Set<Block> seenBlocks, List<Block> allBlocks, HashMap<ResourceKey<CreativeModeTab>, List<SavedBlock>> content) {
		List<SavedBlock> unsortable = new ArrayList<>();
		allBlocks.forEach(block -> {
			if (!seenBlocks.contains(block)) {
				unsortable.add(new SavedBlock(block.defaultBlockState(), null, null));
			}
		});
		unsortable.addAll(0, content.get(CreativeModeTabs.OP_BLOCKS));
		content.put(CreativeModeTabs.OP_BLOCKS, Collections.unmodifiableList(unsortable));
	}

	private static void preparePlatformDependEvent(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, PlatformGuiService.ItemsOutputAccess modReceiver, CreativeModeTab.DisplayItemsGenerator origContent) {
		try {
			PlatformGuiService.INSTANCE.collectItemsFromAllTabs(tab, key, parameters, modReceiver, origContent);
		} catch (Throwable e) {
			PlatformGuiService.INSTANCE.fallbackVanillaTabContent(parameters, modReceiver, origContent);
			MorphUtils.LOGGER.error("Error when collect tabs for external mods with event!", e);
		}
	}

	@Nullable
	private static CompoundTag getTagForBlockEntity(ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (customData != null) {
			CompoundTag tag = customData.copyTagWithoutId();
			if (!tag.isEmpty()) return tag;
		}
		return null;
	}

	private static BlockState prepareBlockStateTag(BlockState blockState, ItemStack item) {
		BlockItemStateProperties properties = item.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		if (!properties.isEmpty()) {
			return properties.apply(blockState);
		}
		return blockState;
	}


	public record Context(FeatureFlagSet flagSet, HolderLookup.Provider holders) {
		public boolean equals(Object obj) {
			if (obj instanceof Context(FeatureFlagSet set, HolderLookup.Provider holders1)) {
				return set.equals(flagSet) && holders1 == holders;
			}
			return false;
		}
	}
}
