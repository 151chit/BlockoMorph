package net.blockomorph.screens.config;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.blockomorph.screens.AbstractScreen;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.screens.utils.ListenerEditBox;
import net.blockomorph.screens.utils.ScrollerManager;
import net.blockomorph.utils.DamageHandler;
import net.blockomorph.utils.config.list.DamageTypeIdsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DamageTypeListOptionEditingScreen extends AbstractScreen implements ConfigSyncListener {
	private static final Identifier DAMAGE_TYPES = GuiUtils.res("textures/screens/damage_types.png");
	private static final int PLATE_VISIBLE, PLATE_HEIGHT, PLATE_WIDTH, PLATE_LEFT, PLATE_UP;
	private final DamageTypeIdsConfig blockIdsSetConfig;
	private final ConfigRenderer.ConfigRenderingContext context;
	private final List<Map.Entry<ResourceKey<DamageType>, DamageType>> renderableDamages = new ArrayList<>(5);
	private final ScrollerManager<Map.Entry<ResourceKey<DamageType>, DamageType>> scrollerManager;
	private final Object2IntMap<Identifier> damageScaleCache = new Object2IntOpenHashMap<>();
	private final ListenerEditBox[] damageScalesFields = new ListenerEditBox[PLATE_VISIBLE];
	private boolean initialized;

	public DamageTypeListOptionEditingScreen(DamageTypeIdsConfig configInstance, ConfigRenderer.ConfigRenderingContext context) {
		super("ids_set_option_screen", null);
		this.blockIdsSetConfig = configInstance;
		this.context = context;
		this.scrollerManager = new ScrollerManager<>(() -> this.leftPos + 158, () -> this.topPos + 16, 143, 1, PLATE_VISIBLE, this.renderableDamages, null) {@Override public void refreshList() {super.refreshList();
			DamageTypeListOptionEditingScreen.this.refreshList();
		}};
		if (GuiUtils.MC.level != null) {
			Registry<DamageType> damageTypes = GuiUtils.MC.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
			this.scrollerManager.setMainList(damageTypes.stream().map(damageType -> {
				ResourceKey<DamageType> key = damageTypes.getResourceKey(damageType).orElseThrow(NullPointerException::new);
				if (!key.equals(DamageHandler.PLAYER_DESTROYED) && !key.equals(DamageHandler.PLAYER_DESTROYED_NULL)) {
					return Map.entry(key, damageType);
				}
				return null;
			}).filter(Objects::nonNull).toList());
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tick) {
		super.render(guiGraphics, mouseX, mouseY, tick);
		var holderIndex = this.getDamageAtPos(mouseX, mouseY, true);
		if (holderIndex != null) {
			this.viewTooltip(this.renderableDamages.get(holderIndex), mouseX, mouseY);
		} else {
			this.renderDamageScaleTooltip();
		}
	}

	@Override
	protected void renderMenu() {
		super.renderMenu();
		this.scrollerManager.renderScroller(this.gui);
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			if (i < this.renderableDamages.size()) {
				Map.Entry<ResourceKey<DamageType>, DamageType> damageHolder = this.renderableDamages.get(i);
				Identifier id = damageHolder.getKey().identifier();
				int x = this.leftPos + PLATE_LEFT;
				int y = this.topPos + PLATE_UP + i * PLATE_HEIGHT;
				boolean active = this.blockIdsSetConfig.getValue().containsKey(id);
				this.gui.blit(DAMAGE_TYPES, x, y, 0, active ? PLATE_HEIGHT : 0, PLATE_WIDTH, PLATE_HEIGHT, 178, 98);
				this.gui.blit(DAMAGE_TYPES, x + 121, y + 5, this.selectXforDamageType(damageHolder.getValue().effects()), 58, 18, 18, 178, 98);
				String name = id.getPath();
				if (this.font.width(name) > 110) {
					name = this.font.plainSubstrByWidth(name, 98) + "..";
				}
				this.gui.drawString(Component.literal(name), x + 5, y + 5, active ? ARGB.color(255, 0, 102, 5) : -1, false);
				if (active) this.gui.drawString(Component.translatable("blockomorph.gui.damageSelector.need_for_damage"), x + 4, y + 16, -1, false);
			}
		}
		this.gui.drawString(Component.translatable("blockomorph.gui.damageSelector.name"), this.leftPos + 8, this.topPos + 6, 4210752, false);
	}

	private void renderDamageScaleTooltip() {
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			var field = this.damageScalesFields[i];
			if (field.isHovered() && field.isActive()) {
				this.gui.renderTooltip(Component.translatable("blockomorph.gui.damageSelector.damageScale"), this.gui.getMouseX(), field.getY());
			}
		}
	}

	private void viewTooltip(Map.Entry<ResourceKey<DamageType>, DamageType> damageHolder, int mouseX, int mouseY) {
		boolean active = this.blockIdsSetConfig.getValue().containsKey(damageHolder.getKey().identifier());
		String deathMessage = "death.attack." + damageHolder.getValue().msgId();
		boolean hasSingle = Language.getInstance().has(deathMessage);
		boolean hasMany = Language.getInstance().has(deathMessage + ".player");
		List<Component> tooltips = new ArrayList<>();
		tooltips.add(Component.literal("Id: ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.literal(damageHolder.getKey().identifier().toString()).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED)));
		if (hasSingle) tooltips.add(Component.literal("Default: ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.translatable(deathMessage, "X", "Y").withStyle(ChatFormatting.GOLD)));
		if (hasMany) tooltips.add(Component.literal("By entity: ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.translatable(deathMessage + ".player", "X", "Y").withStyle(ChatFormatting.GOLD)));
		this.gui.renderTooltip(tooltips, mouseX, mouseY);
	}

	private Integer getDamageAtPos(double mouseX, double mouseY, boolean small) {
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			int x = this.leftPos + PLATE_LEFT;
			int y = this.topPos + PLATE_UP + i * PLATE_HEIGHT;
			if (GuiUtils.isMouseOver(x + (small ? 118 : 0), y, x + 143, y + 29, mouseX, mouseY)) {
				if (i < this.renderableDamages.size()) {
					return i;
				}
			}
		}
		return null;
	}

	private void refreshList() {
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			if (this.damageScalesFields[i] != null && i < this.renderableDamages.size()) {
				Identifier key = this.renderableDamages.get(i).getKey().identifier();
				ListenerEditBox editBox = this.damageScalesFields[i];
				boolean currVisible = this.blockIdsSetConfig.getValue().containsKey(key);
				if (currVisible) {
					String value = this.damageScaleCache.getOrDefault(key, -1) + "";
					if (!value.equals("-1") || (!editBox.getValue().isEmpty() && !editBox.getValue().equals("-")))
						editBox.setValue(value);
					editBox.setVisible(true);
				} else {
					editBox.setVisible(false);
					editBox.setValue("-1");
				}
			}
		}
	}

	private void onDamageScaleEntered(int holderIndex, boolean change) {
		String val = this.damageScalesFields[holderIndex].getValue();
		var holder = this.renderableDamages.get(holderIndex);
		Identifier holderId = holder.getKey().identifier();
		int number = -1;
		boolean has = this.blockIdsSetConfig.getValue().containsKey(holderId);
		if (!has && !change) return;
		if (!change && !val.isEmpty() && !val.equals("-")) {
			var value = this.parseInt(val);
			if (value != null) number = value;
		}
		if (!has) number = this.damageScaleCache.getOrDefault(holderId, -1);
		String action = (change ^ has) ? "+" : "-";
		action = action + " " + holderId;
		action = action + " " + number;
		this.context.onValueChanged().accept(this.blockIdsSetConfig.getName(), action);
	}
	
	private int selectXforDamageType(DamageEffects effects) {
		return switch (effects) {
			case THORNS -> 80;
			case DROWNING -> 20;
			case BURNING -> 40;
			case POKING -> 100;
			case FREEZING -> 60;
			default -> 0;
		};
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double mouseXOffset, double mouseYOffset) {
		if (this.scrollerManager.mouseDragged(mouseButtonEvent.y())) {
			return true;
		}
		return super.mouseDragged(mouseButtonEvent, mouseXOffset, mouseYOffset);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
		if (this.scrollerManager.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y())) {
			return true;
		} else if (!super.mouseClicked(mouseButtonEvent, bl)) {
			var holderIndex = this.getDamageAtPos(mouseButtonEvent.x(), mouseButtonEvent.y(), false);
			if (holderIndex != null) {
				this.onDamageScaleEntered(holderIndex, true);
				GuiUtils.playClickSound();
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
		if (mouseButtonEvent.button() == 0) {
			this.scrollerManager.disableScrollWork();
		}
		return super.mouseReleased(mouseButtonEvent);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double yScrolled) {
		if (this.scrollerManager.mouseScrolled(yScrolled)) {
			return true;
		}
		return super.mouseScrolled(x, y, yScrolled);
	}

	@Override
	protected void init() {
		super.init();
		Button butt = Button.builder(Component.literal("<--"), b -> {
			this.context.onNewScreenRequested().accept(this.context.parentScreen());
		}).pos(this.leftPos + PLATE_LEFT, this.topPos + this.imageHeight + 1).size(20, 20).build();
		this.addRenderableWidget(butt);
		int x = this.leftPos + PLATE_LEFT + 71;
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			ListenerEditBox editBox = this.formEditBox(i, x);
			this.addRenderableWidget(editBox);
			this.damageScalesFields[i] = editBox;
		}
		this.onConfigSynced();
		if (!this.initialized) {
			this.initialized = true;
			for (int i = 0; i < PLATE_VISIBLE; i++) {
				ListenerEditBox editBox = this.damageScalesFields[i];
				if (editBox.getValue().isEmpty()) {
					editBox.setValue("-1");
				}
			}
		}
	}

	private @NotNull ListenerEditBox formEditBox(int i, int x) {
		int y = this.topPos + PLATE_UP + i * PLATE_HEIGHT + 16;
		ListenerEditBox editBox = new ListenerEditBox(this.font, x, y, 30, 10, Component.literal("damageScale"), v -> this.onDamageScaleEntered(i, false), null);
		editBox.setVisible(false);
		editBox.setFilter(value -> {
			if (value.equals(editBox.getValue())) return false;
			if (value.isEmpty() || value.equals("-")) return true;
			Integer val = this.parseInt(value);
			if (val != null) return val >= -1;
			return false;
		});
		return editBox;
	}

	private Integer parseInt(String numberCandidate) {
		try {
			return Integer.parseInt(numberCandidate);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void onConfigSynced() {
		this.damageScaleCache.putAll(this.blockIdsSetConfig.getValue());
		this.scrollerManager.refreshList();
	}

	@Override
	public void resize(int width, int height) {
		ScrollerManager<?> manager = this.scrollerManager;
		float scroll = manager.getScrollerOffset();
		String[] damageScales = new String[PLATE_VISIBLE];
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			damageScales[i] = this.damageScalesFields[i].getValue();
		}
		super.resize(width, height);
		manager.setScrollOffset(scroll);
		manager.refreshList();
		for (int i = 0; i < PLATE_VISIBLE; i++) {
			this.damageScalesFields[i].setValue(damageScales[i]);
		}
	}

	static {
		PLATE_VISIBLE = 5;
		PLATE_HEIGHT = 29;
		PLATE_WIDTH = 144;
		PLATE_LEFT = 10;
		PLATE_UP = 15;
	}
}
