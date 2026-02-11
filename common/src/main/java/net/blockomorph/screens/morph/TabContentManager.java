package net.blockomorph.screens.morph;

import net.blockomorph.screens.morph.tabs.ContentCreativeModeTab;
import net.blockomorph.screens.morph.tabs.SavedBlocksTab;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.accessors.CategoryTab;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
			if (tab.getType() == CreativeModeTab.Type.CATEGORY && tab instanceof CategoryTab acc) {
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
				}, acc.getItemsFormer());
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

	private static void preparePlatformDependEvent(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output, CreativeModeTab.DisplayItemsGenerator orig) {
		try {
			ClientPlatformUtils.INSTANCE.collectItemsFromAllTabs(tab, key, parameters, output, orig);
		} catch (Throwable e) {
			orig.accept(parameters, output);
			MorphUtils.LOGGER.error("Error when collect tabs for external mods with event!", e);
		}
	}

	@Nullable
	private static CompoundTag getTagForBlockEntity(ItemStack stack) {
		return BlockItem.getBlockEntityData(stack);
	}

	private static BlockState prepareBlockStateTag(BlockState blockState, ItemStack item) {
		if (item.getTag() != null) {
			CompoundTag properties = item.getTag().getCompound("BlockStateTag");
			CompoundTag blockstate = NbtUtils.writeBlockState(blockState);
			blockstate.put("Properties", properties);
			return NbtUtils.readBlockState(GuiUtils.MC.level.registryAccess().lookupOrThrow(Registries.BLOCK), blockstate);
		}
		return blockState;
	}


	public record Context(FeatureFlagSet flagSet, HolderLookup.Provider holders) {
		public boolean equals(Object obj) {
			if (obj instanceof Context ctx) {
				return ctx.flagSet.equals(flagSet) && ctx.holders == holders;
			}
			return false;
		}
	}
}
