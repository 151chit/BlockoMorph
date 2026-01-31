package net.blockomorph.screens.config;

import com.google.common.collect.ImmutableList;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ScrollerManager;
import net.blockomorph.utils.config.ConfigInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;

import java.util.ArrayList;
import java.util.List;

public class ConfigRenderableList extends AbstractWidget {
	private final GuiUtils gui = new GuiUtils();
	private final List<RenderableConfigInstance<?>> renderables = new ArrayList<>(7);
	private final List<ConfigRenderer<?>> types = new ArrayList<>();
	protected final ScrollerManager<RenderableConfigInstance<?>> scrollerManager;
	private final int plateHeight;
	private final int plateLength;

	public ConfigRenderableList(List<ConfigInstance<?>> options, ConfigRenderer.ConfigRenderingContext ctx, int x, int y, int length, int height, int barX, int barY, int barHeight, int plateHeight, int plateLength) {
		super(x, y, length, height, CommonComponents.EMPTY);
		ImmutableList.Builder<RenderableConfigInstance<?>> builder = new ImmutableList.Builder<>();
		for (ConfigInstance<?> option : options) {
			if (option.canEditedByOperators()) {
				builder.add(new RenderableConfigInstance<>(option, ctx));
				if (!types.contains(option.getRenderer())) {
					this.types.add(option.getRenderer());
				}
			}
		}
		this.types.forEach(ConfigRenderer::init);
		this.scrollerManager = new ScrollerManager<>(() -> barX, () -> barY, barHeight, 1, 7, this.renderables, null);
		this.scrollerManager.setMainList(builder.build());
		this.plateHeight = plateHeight;
		this.plateLength = plateLength;
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		this.gui.setGuiGraphics(guiGraphics, Minecraft.getInstance().font, mouseX, mouseY, delta);
		this.scrollerManager.renderScroller(this.gui);
		this.renderPlates(false);
		this.renderPlates(true);
		this.renderables.forEach(renderer -> renderer.renderTooltip(this.gui));
	}

	private void renderPlates(boolean mainPhase) {
		for (int i = 0; i < this.renderables.size(); i++) {
			int yStart = this.getY() + i * this.plateHeight;
			this.renderables.get(i).render(this.gui, this.getX(), yStart, this.plateLength, this.plateHeight, mainPhase);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (type == 0) {
			this.types.forEach(ConfigRenderer::startClick);
			for (RenderableConfigInstance<?> instance : this.renderables) {
				if (instance.mouseClicked(mouseX, mouseY)) {
					this.types.forEach(ConfigRenderer::endClick);
					return true;
				}
			}
			this.types.forEach(ConfigRenderer::endClick);
		}
		return this.scrollerManager.mouseClicked(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double xScrolled, double yScrolled) {
		for (RenderableConfigInstance<?> instance : this.renderables) {
			if (instance.mouseScrolled(x, y, yScrolled)) {
				return true;
			}
		}
		return this.scrollerManager.mouseScrolled(yScrolled);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int type, double mouseXOffset, double mouseYOffset) {
		return this.scrollerManager.mouseDragged(mouseY);
	}

	@Override
	public boolean mouseReleased(double x, double y, int type) {
		this.scrollerManager.disableScrollWork();
		return super.mouseReleased(x, y, type);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}

	protected static class RenderableConfigInstance<T extends ConfigInstance<?>> {
		protected final ConfigRenderer.ConfigRenderingContext context;
		private final T instance;
		private final ConfigRenderer<T> renderer;
		private final Rect2i box = new Rect2i(0, 0, 0, 0);

		@SuppressWarnings("unchecked")
		protected RenderableConfigInstance(T instance, ConfigRenderer.ConfigRenderingContext ctx) {
			this.context = ctx;
			this.instance = instance;
			this.renderer = (ConfigRenderer<T>) instance.getRenderer();
		}

		public void render(GuiUtils gui, int x, int y, int length, int height, boolean mainPhase) {
			this.box.setPosition(x, y);
			this.box.setWidth(length);
			this.box.setHeight(height);
			if (mainPhase) {
				this.renderer.render(gui, this.instance, this.box);
			} else {
				this.renderer.renderBackground(gui, this.instance, this.box);
				this.renderer.renderOptionName(gui, this.instance, this.box);
			}
		}

		public void renderTooltip(GuiUtils gui) {
			this.renderer.renderTooltip(gui, this.instance, this.box);
		}

		public boolean mouseClicked(double mouseX, double mouseY) {
			return this.renderer.mouseClicked(this.instance, mouseX, mouseY, this.box, this.context);
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double yOffsetWheel) {
			return this.renderer.mouseScrolled(this.instance, mouseX, mouseY, yOffsetWheel, this.box, this.context);
		}
	}
}
