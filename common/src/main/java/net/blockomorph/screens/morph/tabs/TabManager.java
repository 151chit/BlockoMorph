package net.blockomorph.screens.morph.tabs;

import net.blockomorph.screens.morph.AbstractMorphScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.blockomorph.screens.utils.ScrollerManager;
import net.blockomorph.utils.MorphedBlockEntityProblemReporter;
import net.blockomorph.utils.SavedBlock;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class TabManager {
	private static final Identifier TABS_SPRITE = GuiUtils.res("textures/screens/block_selector_tabs.png");
	public static final int BLOCK_FRAME_SIZE = 36;
	public static final int ROW_WIDTH = 4;
	public static final int ROW_HEIGHT = 4;
	private final List<SavedBlock> renderableBlocks = new ArrayList<>(16) {
		@Override
		public SavedBlock get(int index) {
			if (index >= size() || index < 0) {
				return null;
			}
			return super.get(index);
		}
	};
	public final ScrollerManager<SavedBlock> scrollerManager;
	protected static CreativeModeBlockTab CURRENT_TAB = ContentCreativeModeTab.get(CreativeModeTabs.getDefaultTab());
	private List<CreativeModeBlockTab> CONTENT_TABS = List.of();
	private final Map<CreativeModeBlockTab, Boolean> ALL_SPECIAL_TABS;
	private List<CreativeModeBlockTab> WORKING_SPECIAL_TABS;
	protected static int CURRENT_PAGE = 0;
	private boolean inited;
	protected int ALL_PAGE_COUNT;
	private EditBox searchBox;
	private Button left;
	private Button rigth;
	private final IntSupplier leftPos;
	private final IntSupplier topPos;
	private final int imageLength;
	private final int imageHeight;

	public TabManager(IntSupplier leftPos, IntSupplier topPos, int imageHeight, int imageLength) {
		this.scrollerManager = new ScrollerManager<>(() -> leftPos.getAsInt() + 158, () -> topPos.getAsInt() + 16, 142, 4, 4, this.renderableBlocks, null);
		this.leftPos = leftPos;
		this.topPos = topPos;
		this.imageHeight = imageHeight;
		this.imageLength = imageLength;
		Map<CreativeModeBlockTab, Boolean> specialTabs = new LinkedHashMap<>();
		specialTabs.put(SearchTab.INSTANCE, true);
		specialTabs.put(OperatorTab.INSTANCE, true);
		specialTabs.put(SavedBlocksTab.INSTANCE, true);
		specialTabs.put(AllowedTab.INSTANCE, false);
		this.ALL_SPECIAL_TABS = specialTabs;
	}

	public void changeSpecialTabVisibility(CreativeModeBlockTab tab, boolean value) {
		if (ALL_SPECIAL_TABS.containsKey(tab)) {
			ALL_SPECIAL_TABS.put(tab, value);
			if (this.inited) {
				rebuildSpecialTabs();
				this.trustedSelectTab();
			}
		}
	}

	public void renderBlocksInSlots(GuiUtils gui, AbstractMorphScreen.OnRenderingFrame onRendering) {
		int size = BLOCK_FRAME_SIZE;
		for (int x = 0; x < ROW_WIDTH; x++) {
			for (int y = 0; y < ROW_HEIGHT; y++) {
				int id = y * ROW_HEIGHT + x;
				SavedBlock block = this.renderableBlocks.get(id);
				if (block != null) {
					BlockEntity blockEntity = null;
					try { blockEntity = this.initFakeBE(block);
					} catch (Throwable ignored) {}

					gui.renderBlockInGui(block.getState(), blockEntity, this.leftPos.getAsInt() + 42 + x * size, this.topPos.getAsInt() + 42 + y * size, 20);
					gui.renderAdditionalOnBlock(block.getState(), this.leftPos.getAsInt() + 20 + x * size, this.topPos.getAsInt() + 25 + y * size, 30);
					final int blockX = this.leftPos.getAsInt() + 10 + x * size;
					final int blockY = this.topPos.getAsInt() + 15 + y * size;
					gui.renderInDepthIfNeededAfterBlockRendering(() -> {
						onRendering.render(block, blockX, blockY);
					});
				}
			}
		}
		this.scrollerManager.renderScroller(gui);
	}

	private BlockEntity initFakeBE(SavedBlock block) {
		BlockEntity blockEntity = (block.getState().getBlock() instanceof EntityBlock ent ? ent.newBlockEntity(GuiUtils.AIR, block.getState()) : null);
		if (blockEntity != null && GuiUtils.MC.level != null) {
			blockEntity.setLevel(GuiUtils.MC.level);
			blockEntity.setBlockState(block.getState());
			if (block.getTag() != null) {
				ValueInput tagValueInput = TagValueInput.create(new MorphedBlockEntityProblemReporter(0, null), GuiUtils.MC.level.registryAccess(), block.getTag());
				blockEntity.loadWithComponents(tagValueInput);
			}
		}
		return blockEntity;
	}

	@Nullable
	public SavedBlock getBlockAtPosition(double x, double y) {
		return this.renderableBlocks.get(this.findBlockIndex(x, y));
	}

	public int findBlockIndex(double mouseX, double mouseY) {
		int size = BLOCK_FRAME_SIZE;
		for (int x = 0; x < ROW_WIDTH; x++) {
			for (int y = 0; y < ROW_HEIGHT; y++) {
				int blockX = this.leftPos.getAsInt() + 10 + x * size;
				int blockY = this.topPos.getAsInt() + 15 + y * size;
				if (GuiUtils.isMouseOver(blockX, blockY, blockX + size - 1, blockY + size, mouseX, mouseY)) {
					return y * ROW_HEIGHT + x;
				}
			}
		}
		return -1;
	}

	public void searchBlocks(String searchName) {
		if (this.hasSearchBar()) {
			if (searchName.isEmpty()) {
				this.selectTab(CURRENT_TAB);
			} else {
				List<SavedBlock> list = CURRENT_TAB.getBlockSource();
				if (list != null) {
					this.scrollerManager.setScrollOffset(0f);

					List<SavedBlock> blocks = list.stream().filter(block -> {
						String name;
						if (block.getName() != null) {
							name = block.getName();
						} else name = block.getState().getBlock().getName().getString();
						return name.toLowerCase().contains(searchName.toLowerCase());
					}).toList();
					this.scrollerManager.setMainList(blocks);
					this.scrollerManager.refreshList();
				}
			}
		}
	}

	public boolean mouseClicked(double x, double y, AbstractMorphScreen.OnBlockClick click) {
		SavedBlock block = this.getBlockAtPosition(x, y);
		if (block != null) {
			SoundInstance sound = click.click(block, this.findBlockIndex(x, y), CURRENT_TAB, CURRENT_PAGE);
			if (sound != null) {
				GuiUtils.MC.getSoundManager().play(sound);
			}
			return true;
		} else if (this.scrollerManager.mouseClicked(x, y)) {
			return true;
		} else {
			CreativeModeBlockTab tab = this.getTabAtPosition(x, y);
			if (tab != null) {
				return this.selectTab(tab);
			}
			return false;
		}
	}

	public void renderTabs(GuiUtils gui) {
		this.searchBox.visible = this.hasSearchBar();
		this.renderTabsInGui(gui);
		if (CURRENT_TAB.showTitle())
			gui.drawString(CURRENT_TAB.getDisplayName(), this.leftPos.getAsInt() + 8, this.topPos.getAsInt() + 6, 0x404040, false);
		if (ALL_PAGE_COUNT > 1) {
			Component pageCounter = Component.literal(String.format("%d / %d", CURRENT_PAGE + 1, ALL_PAGE_COUNT));
			gui.drawString(pageCounter, this.leftPos.getAsInt() + (this.imageLength / 2) - (gui.getFont().width(pageCounter) / 2), this.topPos.getAsInt() - 34, -1, true);
		}
	}

	public boolean hasSearchBar() {
		return CURRENT_TAB.hasSearchBar();
	}

	public EditBox getSearchBox() {
		return this.searchBox;
	}

	public void init(Consumer<AbstractWidget> action) {
		int leftPos = this.leftPos.getAsInt();
		int topPos = this.topPos.getAsInt();
		this.left = Button.builder(Component.literal("<"), b -> this.setPage(false)).pos(leftPos - 22, topPos - 22).size(20, 20).build();
		this.rigth = Button.builder(Component.literal(">"), b -> this.setPage(true)).pos(leftPos + this.imageLength, topPos - 22).size(20, 20).build();
		this.left.visible = false;
		this.rigth.visible = false;
		action.accept(this.left);
		action.accept(this.rigth);
		this.searchBox = new ListenerEditBox(GuiUtils.MC.font, this.leftPos.getAsInt() + 99, this.topPos.getAsInt() - 10, 70, 12, Component.translatable("itemGroup.search"), this::searchBlocks, null);
		this.searchBox.setMaxLength(32767);
		this.searchBox.setBordered(false);
		this.searchBox.setTextColor(16777215);
		action.accept(this.searchBox);
		this.inited = true;
	}

	public void rebuildLists(boolean forceChangeTab) {
		List<ContentCreativeModeTab> tabs = new ArrayList<>();
		for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			if (tab.getType() == CreativeModeTab.Type.CATEGORY && tab != OperatorTab.INSTANCE.realTab) {
				ContentCreativeModeTab wrapper = ContentCreativeModeTab.get(tab);
				if (wrapper.needShow()) {
					tabs.add(wrapper);
				}
			}
		}
		CONTENT_TABS = Collections.unmodifiableList(tabs);
		this.rebuildSpecialTabs();
		ALL_PAGE_COUNT = (int) Math.ceil((double) CONTENT_TABS.size() / 10);
		boolean buttonsVisible = ALL_PAGE_COUNT > 1;
		this.left.visible = buttonsVisible;
		this.rigth.visible = buttonsVisible;
		if (this.trustedSelectTab() && forceChangeTab) {
			selectTab(CURRENT_TAB);
		}
	}

	private boolean trustedSelectTab() {
		if (!CONTENT_TABS.contains(CURRENT_TAB) && !WORKING_SPECIAL_TABS.contains(CURRENT_TAB)) {
			selectTab(ContentCreativeModeTab.get(CreativeModeTabs.getDefaultTab()));
			return false;
		}
		return true;
	}

	private void rebuildSpecialTabs() {
		List<CreativeModeBlockTab> specialTabs = new ArrayList<>();
		this.ALL_SPECIAL_TABS.forEach((tab, value) -> {
			if (value && tab.needShow()) {
				specialTabs.add(tab);
			}
		});
		WORKING_SPECIAL_TABS = Collections.unmodifiableList(specialTabs);
	}

	public void forEachTab(Consumer<CreativeModeBlockTab> tab) {
		CONTENT_TABS.forEach(tab);
		WORKING_SPECIAL_TABS.forEach(tab);
	}

	public boolean selectTab(CreativeModeBlockTab tab) {
		List<SavedBlock> list = tab.getBlockSource();
		if (list != null) {
			CreativeModeBlockTab old = CURRENT_TAB;
			CURRENT_TAB = tab;
			ScrollerManager<SavedBlock> manager = this.scrollerManager;
			if (old != tab) manager.setScrollOffset(0f);
			manager.setMainList(list);
			manager.refreshList();
			this.searchBox.setValue("");
			boolean flag = this.hasSearchBar();
			this.searchBox.visible = flag;
			this.searchBox.setCanLoseFocus(!flag);
			this.searchBox.setFocused(flag);
			return true;
		}
		return false;
	}

	private void setPage(boolean up) {
		CURRENT_PAGE = up ? Math.min(CURRENT_PAGE + 1, ALL_PAGE_COUNT - 1) : Math.max(CURRENT_PAGE - 1, 0);
	}

	protected boolean isSelected(boolean special, int i) {
		return CURRENT_TAB.equals((special ? WORKING_SPECIAL_TABS : CONTENT_TABS).get(i));
	}

	protected void renderTabsInGui(GuiUtils gui) {
		this.renderContentTabs(gui);
		this.renderSpecialTabs(gui);
	}

	protected void renderSpecialTabs(GuiUtils gui) {
		for (int i = 0; i < WORKING_SPECIAL_TABS.size(); i++) {
			int tabXSpecial = this.leftPos.getAsInt() + this.imageLength - 38 - i * 32;
			gui.blit(TABS_SPRITE, tabXSpecial, this.getTabY(-1), this.isSelected(true, i) ? 28 : 0, 0, 28, 32, 64, 88);
			this.renderItemInTab(gui, null, i, -1);
		}
	}

	protected void renderContentTabs(GuiUtils gui) {
		int count = 0;
		for (int i = CURRENT_PAGE * 10; i < CURRENT_PAGE * 10 + 10; i++) {
			if (i < CONTENT_TABS.size()) {
				boolean isRight = count >= 5;
				int tabXSpecial = this.leftPos.getAsInt() + (isRight ? this.imageLength - 4 : -28);
				gui.blit(TABS_SPRITE, tabXSpecial, this.getTabY(count), isRight ? 32 : 0, this.isSelected(false, i) ? 60 : 32, 32, 28, 64, 88);
				this.renderItemInTab(gui, isRight, i, count);
				count++;
			} else break;
		}
	}

	protected void renderItemInTab(GuiUtils gui, @Nullable Boolean isRight, int listIndex, int offsetIndex) {
		float tabX;
		boolean isDown = isRight == null;
		if (isDown) {
			tabX = this.leftPos.getAsInt() + this.imageLength - 37 - listIndex * 32 + 5;
		} else {
			tabX = this.leftPos.getAsInt() + (isRight ? this.imageLength + 2 : -19);
		}
		int tabY = this.getTabY(offsetIndex) + (isDown ? 7 : 5);

		ItemStack itemstack = (isDown ? WORKING_SPECIAL_TABS : CONTENT_TABS).get(listIndex).getIconItem();
		gui.renderItem(itemstack, tabX, tabY, 1f, 100);
	}

	public CreativeModeBlockTab getTabAtPosition(double x, double y) {
		int leftPos = this.leftPos.getAsInt();
		int imageWidth = this.imageLength;

		int tabYSpecial = this.getTabY(-1);
		for (int i = 0; i < WORKING_SPECIAL_TABS.size(); i++) {
			int tabXSpecial = leftPos + imageWidth - 38 - i * 32;
			if (x > tabXSpecial && x < tabXSpecial + 28 && y > tabYSpecial && y < tabYSpecial + 32) {
				return WORKING_SPECIAL_TABS.get(i);
			}
		}
		for (int i = 0; i < 10; i++) {

			int tabX = leftPos;
			int tabY = this.getTabY(i);

			if (i < 5) {
				tabX -= 28;
			} else {
				tabX += imageWidth - 4;
			}
			if (x > tabX && x < tabX + 32 && y > tabY && y < tabY + 28) {
				if (10 * CURRENT_PAGE + i < CONTENT_TABS.size())
					return CONTENT_TABS.get(10 * CURRENT_PAGE + i);
			}

		}

		return null;
	}

	protected int getTabY(int i) {
		if (i < 0) return this.topPos.getAsInt() + this.imageHeight - 4; //Special Tabs

		if (i > 4) i -= 5; //right column

		return this.topPos.getAsInt() + 3 + i * 32;
	}
}
