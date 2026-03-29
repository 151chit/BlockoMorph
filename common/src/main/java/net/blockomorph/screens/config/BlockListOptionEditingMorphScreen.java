package net.blockomorph.screens.config;

import net.blockomorph.screens.morph.AbstractMorphScreen;
import net.blockomorph.screens.morph.tabs.CreativeModeBlockTab;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.SavedBlock;
import net.blockomorph.utils.config.list.BlockIdsSetConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

import static net.blockomorph.screens.morph.tabs.TabManager.BLOCK_FRAME_SIZE;

public class BlockListOptionEditingMorphScreen extends AbstractMorphScreen {
	private final BlockIdsSetConfig blockIdsSetConfig;
	private final ConfigRenderer.ConfigRenderingContext context;

	public BlockListOptionEditingMorphScreen(BlockIdsSetConfig configInstance, ConfigRenderer.ConfigRenderingContext context) {
		this.blockIdsSetConfig = configInstance;
		this.context = context;
	}

	@Override
	protected void initAdditional(Consumer<AbstractWidget> action) {
		Button butt = Button.builder(Component.literal("<--"), b -> {
			this.context.onNewScreenRequested().accept(this.context.parentScreen());
		}).pos(this.leftPos + 10, this.topPos + this.imageHeight + 1).size(20, 20).build();
		action.accept(butt);
	}

	@Override
	protected void renderFrame(SavedBlock block, int x, int y) {
		Identifier name = BuiltInRegistries.BLOCK.getKey(block.getState().getBlock());
		boolean contains = this.blockIdsSetConfig.getValue().contains(name);
		if (contains)
			this.gui.blitMonoImage(this.blockIdsSetConfig.getFrameTexture(), x, y, BLOCK_FRAME_SIZE, BLOCK_FRAME_SIZE);
	}

	@Override
	protected SoundInstance onClickOnBlock(SavedBlock block, int number, CreativeModeBlockTab selectedTab, int page) {
		Identifier name = BuiltInRegistries.BLOCK.getKey(block.getState().getBlock());
		boolean contains = this.blockIdsSetConfig.getValue().contains(name);
		this.context.onValueChanged().accept(this.blockIdsSetConfig.getName(), (contains ? "- " : "+ ") + name);
		return GuiUtils.getClickSound();
	}
}
