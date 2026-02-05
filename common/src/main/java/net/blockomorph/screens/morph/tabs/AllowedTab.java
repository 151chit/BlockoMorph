package net.blockomorph.screens.morph.tabs;

import net.blockomorph.screens.morph.TabContentManager;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.accessors.EntityAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AllowedTab extends CreativeModeBlockTab {
	protected static final ResourceKey<CreativeModeTab> ALLOWED_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, GuiUtils.res("allowed_blocks"));
	protected static final CreativeModeTab ALLOWED_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(
			Component.translatable("blockomorph.gui.morphScreen.allowed_tab")).icon(() -> new ItemStack(Items.NETHER_STAR)).build();
	//DEPEND /\
	private static List<SavedBlock> CONTENT = List.of();
	private static TabContentManager.Context LAST_CONTEXT;
	private static boolean DIRTY_CONFIG;
	private static Object LAST_PERMISSION;
	public static final AllowedTab INSTANCE = new AllowedTab();

	private AllowedTab() {
		super(ALLOWED_TAB_KEY, ALLOWED_TAB);
	}

	public static void markDirty() {
		DIRTY_CONFIG = true;
	}

	@Override
	public boolean showTitle() {
		return true;
	}

	@Override
	public boolean hasSearchBar() {
		return true;
	}

	@Override
	public boolean needShow() {
		if (this.checkContentAndStopIfBad()) return false;
		return !CONTENT.isEmpty() && CONTENT.size() != TabContentManager.getAllBlocks().size();
	}

	@Override
	public List<SavedBlock> getBlockSource() {
		if (this.checkContentAndStopIfBad()) return List.of();
		return CONTENT;
	}

	private boolean checkContentAndStopIfBad() {
		if (GuiUtils.MC.player == null) return true;
		if (checkImmutableCondition(LAST_CONTEXT, o -> LAST_CONTEXT = o, TabContentManager::getContext) ||
				DIRTY_CONFIG ||
				checkImmutableCondition(LAST_PERMISSION, o -> LAST_PERMISSION = o, () -> GuiUtils.MC.player.getPermissionLevel())
		) {
			rebuildContent();
			DIRTY_CONFIG = false;
		}
		return false;
	}

	private static <T> boolean checkImmutableCondition(T condition, Consumer<T> setter, Supplier<T> getter) {
		if (condition == null || !condition.equals(getter.get())) {
			setter.accept(getter.get());
			return true;
		}
		return false;
	}

	@Override
	public void onConfigChanged() {
		rebuildContent();
	}

	@Override
	public void onPermissionChanged() {
		rebuildContent();
	}

	private static void rebuildContent() {
		PlayerAccessor player = PlayerAccessor.of(GuiUtils.MC.player);
		Set<BlockState> set = TabContentManager.getAllBlocks().stream().map(Block::defaultBlockState).collect(Collectors.toSet());
		BannedBlock.filterBlocksWithoutReasons(set, player, BannedBlock.Source.SYSTEM);
		CONTENT = set.stream().map(block -> new SavedBlock(block, null, null)).toList();
	}
}
