package net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.blockstate;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.blockomorph.screens.morphConfig.propertiesWidget.BlockStatePropsRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class BlockStateSelectorOverlay extends TagEditingOverlay {
	private final Consumer<BlockState> handler;
	private BlockState state;
	private BlockEntity tempBE;
	private int leftPos;
	private int topPos;
	private static final ResourceLocation MENU = GuiUtils.res("textures/screens/state_selector.png");
	private BlockStatePropsRenderer propsRenderer;
	private Button exit;
	private Button typeEdit;

	public BlockStateSelectorOverlay(BlockState state, Consumer<BlockState> handler) {
		super(159, 131);
		this.handler = handler;
		this.state = state;
		this.setTempBE();
	}

	private void setTempBE() {
		this.tempBE = this.state.getBlock() instanceof EntityBlock ent ? ent.newBlockEntity(GuiUtils.AIR, this.state) : null;
		if (this.tempBE != null) {
			this.tempBE.setLevel(GuiUtils.MC.level);
		}
	}

	@Override
	public void renderInGui(GuiUtils gui) {
		gui.renderBlockInGui(this.state, this.tempBE, this.leftPos + 63.85f, this.topPos + 64f, 36f);
		gui.renderAdditionalOnBlock(this.state, this.leftPos + 30.5f, this.topPos + 40.5f, 55f);
		this.propsRenderer.render(gui);
		String[] text = Component.translatable("blockomorph.gui.stateSelectorOverlay.selectBlock").getString().split("\n");
		int x = this.leftPos + 8 + (this.typeEdit.getWidth() / 2);
		gui.drawCenteredString(Component.literal(text[0]), x, this.topPos + 84, -1, true);
		gui.drawCenteredString(Component.literal(text[1]), x, this.topPos + 93, -1, true);

		if (GuiUtils.isMouseOver(this.leftPos + 8, this.topPos + 18, this.leftPos + 69, this.topPos + 79, gui.getMouseX(), gui.getMouseY())) {
			gui.renderTooltip(this.state.getBlock().getName(), gui.getMouseX(), gui.getMouseY());
		}
	}

	@Override
	protected void renderBackground(GuiUtils gui) {
		gui.blitMonoImage(MENU, this.leftPos, this.topPos, this.imageLength, this.imageHeight);
		this.renderString(gui);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (type == 0) {
			if (super.mouseClicked(mouseX, mouseY, type)) {
				return true;
			}
			return this.propsRenderer.mouseClicked(mouseX, mouseY, type);
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xWheelOffset, double yWheelOffset) {
		return this.propsRenderer.mouseScrolled(mouseX, mouseY, yWheelOffset);
	}

	private void renderString(GuiUtils gui) {
		String blockName = this.state.getBlock().getName().getString();
		if (gui.getFont().width(blockName) > 68) {
			blockName = gui.getFont().plainSubstrByWidth(blockName, 62) + "...";
		}
		gui.drawString(Component.literal(blockName), this.leftPos + 6, this.topPos + 6, 4210752, false);
		gui.drawString(Component.literal("BlockState"), this.leftPos + 83, this.topPos + 11, 4210752, false);
	}

	private void changeBlockState(BlockState newState) {
		this.state = newState;
		this.setTempBE();
		this.handler.accept(newState);
	}

	@Override
	public void init(int width, int height, Consumer<TagEditingOverlay> onChange) {
		super.init(width, height, onChange);
		this.leftPos = (this.width - this.imageLength) / 2;
		this.topPos = (this.height - this.imageHeight) / 2;
		this.propsRenderer = new BlockStatePropsRenderer(this.leftPos + 79, this.topPos + 20, 5, () -> this.state, this::changeBlockState);
		this.exit = Button.builder(CommonComponents.GUI_DONE, b -> {
			onChange.accept(null);
		}).bounds(this.leftPos + 8, this.topPos + 105, 61, 20).build();
		this.addRenderableWidget(this.exit);
		this.typeEdit = Button.builder(CommonComponents.EMPTY, b -> {
			onChange.accept(new BlockTypeSelectorOverlay(block -> {
				onChange.accept(this);
				this.changeBlockState(block);
			}));
		}).bounds(this.leftPos + 8, this.topPos + 82, 61, 20).build();
		this.addRenderableWidget(this.typeEdit);
	}
}
