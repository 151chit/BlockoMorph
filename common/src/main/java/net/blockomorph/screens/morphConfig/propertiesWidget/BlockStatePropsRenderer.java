package net.blockomorph.screens.morphConfig.propertiesWidget;

import net.blockomorph.screens.morphConfig.propertiesWidget.renderers.BooleanPropertyRenderer;
import net.blockomorph.screens.morphConfig.propertiesWidget.renderers.EnumPropertyRenderer;
import net.blockomorph.screens.morphConfig.propertiesWidget.renderers.IntegerPropertyRenderer;
import net.blockomorph.screens.morphConfig.propertiesWidget.renderers.UnknownPropertyRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ScrollerManager;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockStatePropsRenderer {
	private final HashMap<Class<?>, PropertyRenderer<?, ?>> RENDERERS = new HashMap<>();
	private final int x, y;
	protected static final int PLATE_LENGTH = 67;
	protected static final int PLATE_HEIGHT = 19;
	private final Consumer<BlockState> newStateHandler;
	private final List<RenderableProperty<?, ?>> renderableProperties;
	private final ScrollerManager<RenderableProperty<?, ?>> scrollerManager;
	private final Supplier<BlockState> stateSource;
	private final int maxPlatesCount;
	private BlockState currentState;

	public BlockStatePropsRenderer(int x, int y, int maxPlatesCount, Supplier<BlockState> stateSource, Consumer<BlockState> newStateHandler) {
		this.x = x;
		this.y = y;
		this.maxPlatesCount = maxPlatesCount;
		this.stateSource = stateSource;
		this.newStateHandler = newStateHandler;
		this.renderableProperties = new ArrayList<>(maxPlatesCount);
		this.scrollerManager = new ScrollerManager<>(null, null, 0, 1, maxPlatesCount, this.renderableProperties, null);
		RENDERERS.put(BooleanProperty.class, new BooleanPropertyRenderer());
		RENDERERS.put(IntegerProperty.class, new IntegerPropertyRenderer());
		RENDERERS.put(EnumProperty.class, new EnumPropertyRenderer<>());
		RENDERERS.put(Property.class, new UnknownPropertyRenderer<>());
	}

	public void render(GuiUtils gui) {
		BlockState state = this.stateSource.get();
		if (!state.equals(this.currentState)) {
			this.setBlockState(state);
		}
		this.renderProperties(gui, false);
		this.renderProperties(gui, true);
		for (RenderableProperty<?, ?> renderableProperty : this.renderableProperties) {
			if (renderableProperty.renderTooltip(gui)) {
				break;
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		if (type == 0) {
			RENDERERS.forEach((clazz, render) -> render.startClick());
			for (RenderableProperty<?, ?> instance : this.renderableProperties) {
				if (instance.mouseClicked(mouseX, mouseY)) {
					RENDERERS.forEach((clazz, render) -> render.endClick());
					return true;
				}
			}
			RENDERERS.forEach((clazz, render) -> render.endClick());
		}
		return false;
	}

	public boolean mouseScrolled(double x, double y, double yScrolled) {
		for (RenderableProperty<?, ?> instance : this.renderableProperties) {
			if (instance.mouseScrolled(x, y, yScrolled)) {
				return true;
			}
		}
		if (GuiUtils.isMouseOver(this.x, this.y, this.x + PLATE_LENGTH, this.y + PLATE_HEIGHT * maxPlatesCount, x, y)) {
			return this.scrollerManager.mouseScrolled(yScrolled);
		}
		return false;
	}

	private void renderProperties(GuiUtils gui, boolean mainPhase) {
		for (int i = 0; i < this.renderableProperties.size(); i++) {
			int yStart = i * PLATE_HEIGHT;
			this.renderableProperties.get(i).render(gui, mainPhase, yStart);
		}
	}

	private void setBlockState(BlockState state) {
		if (this.currentState == null || !state.is(this.currentState.getBlock()))
			this.scrollerManager.setScrollOffset(0f);
		this.currentState = state;
		List<RenderableProperty<?, ?>> properties = new ArrayList<>();
		for (Property<?> property : this.currentState.getProperties()) {
			properties.add(new RenderableProperty<>(property));
		}
		this.scrollerManager.setMainList(properties);
		this.scrollerManager.refreshList();
	}

	private class RenderableProperty<VALUE extends Comparable<VALUE>, T extends Property<VALUE>> {
		private final Rect2i box = new Rect2i(0, 0, 0, 0);
		private final PropertyRenderer<VALUE, T> renderer;
		private final T property;
		private final VALUE value;

		@SuppressWarnings("unchecked")
		private RenderableProperty(T property) {
			this.property = property;
			this.value = BlockStatePropsRenderer.this.currentState.getValue(property);
			Class<?> clazz = property.getClass();
			while (true) {
				PropertyRenderer<VALUE, T> renderer = (PropertyRenderer<VALUE, T>) RENDERERS.get(clazz);
				if (renderer != null) {
					this.renderer = renderer;
					break;
				} else {
					clazz = clazz.getSuperclass();
				}
			}
		}

		public void render(GuiUtils gui, boolean mainPhase, int yOffset) {
			this.box.setPosition(BlockStatePropsRenderer.this.x, BlockStatePropsRenderer.this.y + yOffset);
			this.box.setWidth(PLATE_LENGTH);
			this.box.setHeight(PLATE_HEIGHT);
			if (mainPhase) {
				this.renderer.render(gui, this.property, this.value, this.box);
			} else {
				this.renderer.renderBackground(gui, this.property, this.value, this.box);
				this.renderer.renderPropertyName(gui, this.property, this.box);
			}
		}

		public boolean renderTooltip(GuiUtils gui) {
			return this.renderer.renderTooltip(gui, this.property, this.value, this.box);
		}

		public boolean mouseClicked(double mouseX, double mouseY) {
			return this.renderer.mouseClicked(BlockStatePropsRenderer.this.newStateHandler, BlockStatePropsRenderer.this.currentState, this.property, this.value, mouseX, mouseY, this.box);
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double yOffsetWheel) {
			return this.renderer.mouseScrolled(BlockStatePropsRenderer.this.newStateHandler, BlockStatePropsRenderer.this.currentState, this.property, this.value, mouseX, mouseY, yOffsetWheel, this.box);
		}
	}
}
