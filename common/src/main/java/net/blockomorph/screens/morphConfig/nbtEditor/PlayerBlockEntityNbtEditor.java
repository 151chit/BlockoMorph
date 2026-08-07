package net.blockomorph.screens.morphConfig.nbtEditor;

import net.blockomorph.network.ServerBoundBlockMorphPacket;
import net.blockomorph.network.ServerBoundSelfNbtRequestPacket;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.SpriteImageButton;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PlayerBlockEntityNbtEditor extends NbtEditorScreen implements ConfigSyncListener {
	private static final Identifier BUTTON_REFRESH = GuiUtils.res("textures/screens/nbt_request.png");
	protected boolean init;
	private final Screen parentScreen;
	private Button exit;

	public PlayerBlockEntityNbtEditor(Screen old) {
		this.parentScreen = old;
	}

	private PlayerAccessor getPlayer() {
		return PlayerAccessor.of(GuiUtils.MC.player);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
		if (this.exit.mouseClicked(mouseButtonEvent, doubleClick)) {
			return true;
		}
		return super.mouseClicked(mouseButtonEvent, doubleClick);
	}

	@Override
	protected void onTagEdited(CompoundTag tag) {
		MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(this.getPlayer().getBlockState(InPlayerBlockPos.ZERO), tag));
		this.setError(null);
	}

	@Override
	protected void init() {
		super.init();
		if (!this.init) {
			MorphUtils.sendServer(ServerBoundSelfNbtRequestPacket.INSTANCE);
			this.init = true;
		}

		SpriteImageButton refreshButton = new SpriteImageButton(this.leftPos + 156, this.topPos + 6, 16, 16, BUTTON_REFRESH, b -> {
			MorphUtils.sendServer(ServerBoundSelfNbtRequestPacket.INSTANCE);
			this.setNewTag(null);
		}, null, false);
		refreshButton.setTooltip(Tooltip.create(Component.translatable("blockomorph.gui.nbtEditor.blockEntity.button.refresh.tooltip")));
		this.addRenderableWidget(refreshButton);

		Button done = Button.builder(Component.literal("<--"), b -> {
			GuiUtils.MC.setScreen(this.parentScreen);
		}).pos(this.leftPos - 25, this.topPos).size(20, 20).build();
		this.addRenderableWidget(done);
		this.exit = done;
	}

	@Override
	public void onConfigSynced() {
		if (BannedBlock.isBannedBlock(this.getPlayer().getBlockState(InPlayerBlockPos.ZERO), this.getPlayer(), BannedBlock.Source.NETWORK) != null) {
			this.onClose();
		} else {
			ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(GuiUtils.MC.player);
			if (!access.config) {
				this.onClose();
			}
		}
	}
}
