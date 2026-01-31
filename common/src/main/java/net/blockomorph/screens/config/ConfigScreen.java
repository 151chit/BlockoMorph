package net.blockomorph.screens.config;

import net.blockomorph.network.ServerBoundConfigUpdatePacket;
import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.morphConfig.nbtEditor.NbtPath;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigScreen extends AbstractScreen {
	private ConfigRenderableList configList;
	private final NbtPath path = new NbtPath.RootNbtPath();
	private final ConfigRenderer.ConfigRenderingContext context = new ConfigRenderer.ConfigRenderingContext(this, (name, value) -> {
		MorphUtils.sendServer(new ServerBoundConfigUpdatePacket(name, value));
	}, GuiUtils.MC::setScreen, option -> {
		this.path.append(option.getName());
		this.rebuildOptions();
	});
	private Button pathExit;

	public ConfigScreen() {
		super("config_screen", null);
	}

	@Override
	public void renderMenu() {
		super.renderMenu();
		String name = Component.translatable("menu.options").getString();
		name = name.replace("...", "");
		if (!this.path.isEmpty()) {
			name = name + ": " + this.path.getVisualString();
		}
		this.pathExit.render(this.gui.getGuiGraphics(), this.gui.getMouseX(), this.gui.getMouseY(), this.gui.getTick());
		this.gui.drawString(Component.literal(name), this.leftPos + 8, this.topPos + 6, 4210752, false);
	}

	private void rebuildOptions() {
		this.pathExit.visible = !this.path.isEmpty();
		List<ConfigInstance<?>> list = Config.get().RECURSIVE_OPTIONS;
		for (String folder : this.path.constructPath(new ArrayList<>())) {
			list = this.getOptionByName(folder, list).getEnterableInstances();
		}
		this.configList = new ConfigRenderableList(list, this.context, this.leftPos + 10, this.topPos + 15, 144, this.height - this.topPos, this.leftPos + 158, this.topPos + 16, 142, 20, 144);
		this.clearWidgets();
		this.addRenderableWidget(this.configList);
		this.configList.scrollerManager.refreshList();
	}

	private ConfigInstance<?> getOptionByName(String name, List<ConfigInstance<?>> list) {
		for (ConfigInstance<?> configInstance : list) {
			if (configInstance.getName().equals(name)) {
				return configInstance;
			}
		}
		throw new IllegalArgumentException("Config path is wrong! " + this.path);
	}

	@Override
	public GuiEventListener getFocused() {
		return this.configList;
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double d, double e) {
		return Optional.of(this.configList);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (this.pathExit.mouseClicked(mouseX, mouseY, type)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, type);
	}

	@Override
	protected void init() {
		super.init();
		this.pathExit = Button.builder(Component.literal("<--"), button -> {
			this.path.back();
			this.rebuildOptions();
		}).size(22, 9).pos(this.leftPos + 146, this.topPos + 4).build();
		this.pathExit.visible = !this.path.isEmpty();
		float scrollOff = 0f;
		if (this.configList != null) {
			scrollOff = this.configList.scrollerManager.getScrollerOffset();
		}
		this.rebuildOptions();
		this.configList.scrollerManager.setScrollOffset(scrollOff);
		this.configList.scrollerManager.refreshList();
	}
}
