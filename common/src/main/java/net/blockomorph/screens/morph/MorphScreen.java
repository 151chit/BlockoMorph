package net.blockomorph.screens.morph;

import net.blockomorph.network.ServerBoundBlockMorphPacket;
import net.blockomorph.screens.morph.tabs.AllowedTab;
import net.blockomorph.screens.morph.tabs.CreativeModeBlockTab;
import net.blockomorph.screens.morph.tabs.SavedBlocksTab;
import net.blockomorph.screens.morphConfig.MorphConfigScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.SpriteImageButton;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static net.blockomorph.screens.morph.tabs.TabManager.BLOCK_FRAME_SIZE;

public class MorphScreen extends AbstractMorphScreen {
	private static final ResourceLocation UNMORPH_BUTTON_SPRITE = GuiUtils.res("textures/screens/unmorph_but.png");
	private static final ResourceLocation FUSE_BUTTON_SPRITE = GuiUtils.res("textures/screens/flame_but.png");
	private static final ResourceLocation LOCK_FRAME = GuiUtils.res("textures/screens/sel_lock.png");
	private static final ResourceLocation SELECTED_FRAME = GuiUtils.res("textures/screens/selected.png");
	private static final ResourceLocation MODE_TABS = GuiUtils.res("textures/screens/exit_tabs.png");
	private SpriteImageButton fuseButton;
	private PlayerAccessor player;
	private boolean needUpperTabs;

	@Override
	public void tick() {
		this.player = PlayerAccessor.of(GuiUtils.MC.player);
	}

	public MorphScreen() {
		this.tick();
		this.tabManager.changeSpecialTabVisibility(AllowedTab.INSTANCE, true);
	}

	@Override
	protected void initAdditional(Consumer<AbstractWidget> action) {
		action.accept(new SpriteImageButton(leftPos + 10, topPos + this.imageHeight + 1, 26, 26, UNMORPH_BUTTON_SPRITE, button -> {
			MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(Blocks.AIR.defaultBlockState(), null));
		}, () -> this.player.isBlockomorphFullActive(), true));

		action.accept(this.fuseButton = new SpriteImageButton(leftPos - 28, topPos + this.imageHeight + 1, 26, 26, FUSE_BUTTON_SPRITE, button -> {
			MorphUtils.sendServer(ServerBoundBlockMorphPacket.fuseTnt());
		}, () -> this.player.getActiveMorphTnt() == null, true));
		this.fuseButtonVisibilityCheck();
	}

	@Override
	protected void init() {
		super.init();
		this.checkSavedTab();
		this.setUseUpperTabs(MorphUtils.getScreenAccess(this.player.player()).config);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
		super.render(guiGraphics, mouseX, mouseY, tick);
		this.fuseButtonVisibilityCheck();
	}

	@Override
	protected void renderMenu() {
		super.renderMenu();
		if (this.needUpperTabs) {
			this.gui.blit(MODE_TABS, this.leftPos + 4, this.topPos - 19, 0, 0, 80, 22, 80, 46);
		}
	}

	protected void setUseUpperTabs(boolean yes) {
		this.needUpperTabs = yes;
	}

	@Override
	public boolean mouseClicked(double x, double y, int type) {
		if (this.needUpperTabs) {
			if (x > this.leftPos + 4 && x < this.leftPos + 4 + 41 && y > this.topPos - 19 && y < this.topPos - 19 + 22) {
				GuiUtils.MC.setScreen(new MorphConfigScreen());
				return true;
			}
		}
		return super.mouseClicked(x, y, type);
	}

	private void fuseButtonVisibilityCheck() {
		this.fuseButton.visible = this.player.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof TntBlock;
	}

	@Nullable
	private BannedBlock isBannedBlock(SavedBlock block) {
		if (!MorphUtils.getScreenAccess(this.player.player()).config && (block.getTag() != null || !block.getState().getBlock().defaultBlockState().equals(block.getState())))
			return new BannedBlock("You cannot morph into configured block!",
					Component.translatable("blockomorph.bannedBlock.configured"));
		return BannedBlock.isBannedBlock(block.getState(), this.player, BannedBlock.Source.NETWORK);
	}

	@Override
	public void onOperatorRightsChanged() {
		this.onConfigSynced();
	}

	@Override
	public void onConfigSynced() {
		super.onConfigSynced();
		this.checkSavedTab();
		this.setUseUpperTabs(MorphUtils.getScreenAccess(this.player.player()).config);
	}

	private void checkSavedTab() {
		this.tabManager.changeSpecialTabVisibility(SavedBlocksTab.INSTANCE, MorphUtils.getScreenAccess(this.player.player()).config);
	}

	@Override
	protected SoundInstance onClickOnBlock(SavedBlock block, int number, CreativeModeBlockTab selectedTab, int page) {
		if (this.isBannedBlock(block) == null) {
			MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(block.getState(), block.getTag()));
			return GuiUtils.getClickSound();
		}
		return null;
	}

	@Override
	protected void renderFrame(SavedBlock block, int x, int y) {
		BlockState state = block.getState();
		BannedBlock ban = this.isBannedBlock(block);
		if (ban != null) {
			gui.blitMonoImage(LOCK_FRAME, x, y, BLOCK_FRAME_SIZE, BLOCK_FRAME_SIZE);
			return;
		}

		BlockState playerState = this.player.getBlockState(InPlayerBlockPos.ZERO);
		CompoundTag tg = this.player.getTag(InPlayerBlockPos.ZERO);

		if (state.equals(playerState)) {
			if (block.getTag() == null || tg == null || tg.equals(block.getTag())) {
				gui.blitMonoImage(SELECTED_FRAME, x, y, BLOCK_FRAME_SIZE, BLOCK_FRAME_SIZE);
			}
		}
	}

	@Override
	protected void renderTooltipForBlock() {
		SavedBlock block = this.tabManager.getBlockAtPosition(gui.getMouseX(), gui.getMouseY());
		if (block != null) {
			BannedBlock ban = this.isBannedBlock(block);
			if (ban != null) {
				Component name = block.getName() == null ? block.getState().getBlock().getName() : Component.literal(block.getName());
				List<Component> hints = List.of(name, Component.literal(ChatFormatting.RED + ban.text().getString()));
				gui.renderTooltip(hints, gui.getMouseX(), gui.getMouseY());
			} else {
				super.renderTooltipForBlock();
			}
		}
	}
}
