package net.blockomorph.screens.morphConfig;

import net.blockomorph.network.ServerBoundBlockMorphPacket;
import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.morph.tabs.SavedBlocksTab;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.morph.MorphScreen;
import net.blockomorph.screens.morphConfig.nbtEditor.PlayerBlockEntityNbtEditor;
import net.blockomorph.screens.morphConfig.propertiesWidget.BlockStatePropsRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.blockomorph.screens.utils.SpriteImageButton;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MorphConfigScreen extends AbstractScreen implements ConfigSyncListener {
	private static final ResourceLocation EXIT_TABS_SPRITE = GuiUtils.res("textures/screens/exit_tabs.png");
	private static final ResourceLocation SAVE_BUTTON_SPRITE = GuiUtils.res("textures/screens/save_but.png");
	private static final ResourceLocation DELETE_BUTTON_SPRITE = GuiUtils.res("textures/screens/edit_bucket.png");
	private static final ResourceLocation NBT_BUTTON_SPRITE = GuiUtils.res("textures/screens/nbt_but.png");
	private BlockStatePropsRenderer propertiesRenderer;
	private final GuiUtils gui = new GuiUtils();
	protected PlayerAccessor player;
	private EditBox saveBox;
	private SpriteImageButton saveButton;
	private SpriteImageButton deleteButton;
	private boolean needUpperTabs;

	public MorphConfigScreen() {
		super("morph_config_screen", null);
		this.player = PlayerAccessor.of(GuiUtils.MC.player);
		SavedBlocksTab.CONTENT_MANAGER.load();
	}

	@Override
	public void onConfigSynced() {
		if (BannedBlock.isBannedBlock(this.getState(), this.player, BannedBlock.Source.NETWORK) != null) {
			this.onClose();
			return;
		}
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(this.player.player());
		if (!access.config) {
			this.onClose();
		} else {
			this.needUpperTabs = access.morph;
		}
	}

	@Override
	public void onOperatorRightsChanged() {
		this.onConfigSynced();
	}

	@Override
	public void tick() {
		this.player = PlayerAccessor.of(GuiUtils.MC.player);
	}

	private BlockState getState() {
		return this.player.getBlockState(InPlayerBlockPos.ZERO);
	}

	@Nullable
	private BlockEntity getBE() {
		return this.player.getBlockEntity(InPlayerBlockPos.ZERO);
	}

	private CompoundTag getTag() {
		CompoundTag tag = new CompoundTag();
		if (this.getBE() != null) tag = this.getBE().saveWithoutMetadata();
		return tag;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
		this.gui.setGuiGraphics(guiGraphics, this.font, mouseX, mouseY, tick);
		super.render(guiGraphics, mouseX, mouseY, tick);
		this.gui.renderBlockInGui(this.getState(), this.getBE(), this.leftPos + 71, this.topPos + 64, 36f);
		this.gui.renderAdditionalOnBlock(this.getState(), this.leftPos + 37.5f, this.topPos + 40.5f, 55f);
		this.propertiesRenderer.render(this.gui);
		if (GuiUtils.isMouseOver(this.leftPos + 15, this.topPos + 18, this.leftPos + 75, this.topPos + 79, mouseX, mouseY)) {
			this.gui.renderTooltip(this.getState().getBlock().getName(), mouseX, mouseY);
		}
	}

	private void renderStrings() {
		String blockName = this.getState().getBlock().getName().getString();
		if (this.font.width(blockName) > 81) {
			blockName = this.font.plainSubstrByWidth(blockName, 77) + "...";
		}
		int color = 4210752;
		this.gui.drawString(Component.literal(blockName), this.leftPos + 6, this.topPos + 6, color, false);
		this.gui.drawCenteredString(Component.literal("BlockState"), this.leftPos + 126, this.topPos + 15, color, false);
		this.gui.drawString(Component.translatable("blockomorph.gui.morphConfigScreen.save"), this.leftPos + 9, this.topPos + 128, color, false);
		this.gui.drawCenteredString(Component.translatable("blockomorph.gui.morphConfigScreen.extended"), this.leftPos + 44, this.topPos + 87, color, false);
	}

	@Override
	public void renderMenu() {
		super.renderMenu();
		if (this.needUpperTabs)
			this.gui.blit(EXIT_TABS_SPRITE, this.leftPos + 4, this.topPos - 19, 0, 23, 80, 22, 80, 46);
		this.renderStrings();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (this.needUpperTabs && GuiUtils.isMouseOver(this.leftPos + 41, this.topPos - 19, this.leftPos + 84, this.topPos + 3, mouseX, mouseY)) {
			GuiUtils.MC.setScreen(new MorphScreen());
			return true;
		} else if (this.propertiesRenderer.mouseClicked(mouseX, mouseY, type)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, type);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double yWheelOffset) {
		if (this.propertiesRenderer.mouseScrolled(mouseX, mouseY, yWheelOffset)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, yWheelOffset);
	}

	@Override
	protected void init() {
		super.init();
		this.propertiesRenderer = new BlockStatePropsRenderer(this.leftPos + 93, this.topPos + 24, 5, this::getState, newState -> {
			MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(newState, null));
		});
		EditBox old = this.saveBox;
		this.saveBox = new ListenerEditBox(this.font, this.leftPos + 7, this.topPos + 137, 129, 19, Component.translatable("blockomorph.gui.morphConfigScreen.save"), value -> this.checkSaveButtons(), ListenerEditBox.EDITBOX_BORDER_SPRITE);
		this.saveBox.setMaxLength(500);
		this.addRenderableWidget(this.saveBox);
		this.saveButton = new SpriteImageButton(this.leftPos + 142, this.topPos + 133, 26, 26, SAVE_BUTTON_SPRITE, button -> {
			SavedBlocksTab.CONTENT_MANAGER.add(new SavedBlock(this.getState(), this.getTag(), this.saveBox.getValue()));
			this.checkSaveButtons();
		}, null, true);
		this.addRenderableWidget(this.saveButton);
		this.deleteButton = new SpriteImageButton(this.leftPos + 142, this.topPos + 133, 26, 26, DELETE_BUTTON_SPRITE, button -> {
			SavedBlocksTab.CONTENT_MANAGER.delete(this.saveBox.getValue());
			this.checkSaveButtons();
		}, null, false);
		SpriteImageButton nbtButton = new SpriteImageButton(this.leftPos + 31, this.topPos + 95, 26, 26, NBT_BUTTON_SPRITE, but -> {
			GuiUtils.MC.setScreen(new PlayerBlockEntityNbtEditor(this));
		}, () -> this.getState().getBlock() instanceof EntityBlock, true);
		this.addRenderableWidget(nbtButton);
		this.addRenderableWidget(this.deleteButton);
		this.suggestSavedBlock(old);
		this.needUpperTabs = MorphUtils.getScreenAccess(this.player.player()).morph;
	}

	private void suggestSavedBlock(EditBox old) {
		if (old == null) {
			BlockState state = this.getState();
			CompoundTag tag = this.getTag();
			for (SavedBlock block : SavedBlocksTab.CONTENT_MANAGER.get().values()) {
				if (block.getState().equals(state)) {
					if (block.getTag().equals(tag)) {
						this.saveBox.setValue(block.getName());
						break;
					}
				}
			}
		} else {
			this.saveBox.setValue(old.getValue());
		}
		this.checkSaveButtons();
	}

	private void checkSaveButtons() {
		String saveBoxContent = this.saveBox.getValue();
		if (saveBoxContent.isEmpty()) {
			this.diactiveSave();
			return;
		}
		SavedBlock savedBlock = SavedBlocksTab.CONTENT_MANAGER.get().get(saveBoxContent);
		if (savedBlock == null) {
			this.saveButton.visible = true;
			this.deleteButton.visible = false;
		} else {
			if (savedBlock.getState().equals(this.getState()) && savedBlock.getTag().equals(this.getTag())) {
				this.saveButton.visible = false;
				this.deleteButton.visible = true;
			} else {
				this.diactiveSave();
				return;
			}
		}
		this.saveButton.active = true;
	}

	private void diactiveSave() {
		this.saveButton.active = false;
		this.saveButton.visible = true;
		this.deleteButton.visible = false;
	}
}
