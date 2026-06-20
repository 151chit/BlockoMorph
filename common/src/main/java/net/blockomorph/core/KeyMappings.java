package net.blockomorph.core;

import net.blockomorph.screens.config.ConfigScreen;
import net.blockomorph.screens.morph.MorphScreen;
import net.blockomorph.screens.morphConfig.MorphConfigScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;


public class KeyMappings {
	private static final Minecraft mc = Minecraft.getInstance();
	private static final ArrayList<KeyMapping> KEYS = new ArrayList<>();

	public static final KeyMapping MORPH = new HandlerKeymapping("morph_menu", GLFW.GLFW_KEY_Y, () -> {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(mc.player);
		if (access.morph) {
			mc.setScreen(new MorphScreen().ignoreInitInput());
			return true;
		}
		return false;
	});

	public static final KeyMapping MORPH_CONFIG = new HandlerKeymapping("morph_config_menu", GLFW.GLFW_KEY_U, () -> {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(mc.player);
		if (access.config && mc.player != null) {
			BlockState blockState = PlayerAccessor.of(mc.player).getBlockState(InPlayerBlockPos.ZERO);
			if (BannedBlock.isBannedBlock(blockState, PlayerAccessor.of(mc.player), BannedBlock.Source.NETWORK) != null) {
				return false;
			}
			mc.setScreen(new MorphConfigScreen());
			return true;
		}
		return false;
	});

	public static final KeyMapping CONFIG = new HandlerKeymapping("config_menu", GLFW.GLFW_KEY_N, () -> {
		if (mc.player != null && mc.player.hasPermissions(2) && Config.get().canOperatorModifyConfig.getValue()) {
			mc.setScreen(new ConfigScreen());
			return true;
		}
		return false;
	});

	public static void registerKeyMappings(Consumer<KeyMapping> register) {
		for (KeyMapping key : KEYS) {
			register.accept(key);
		}
	}

	private static class HandlerKeymapping extends KeyMapping {
		private final BooleanSupplier action;

		public HandlerKeymapping(String lang, int key, BooleanSupplier action) {
			super("blockomorph.key." + lang, key, CATEGORY_INTERFACE);
			KEYS.add(this);
			this.action = action;
		}

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && mc.screen == null) {
				if (!this.action.getAsBoolean() && mc.level != null) {
					GuiUtils.pushHotbarMessage(Component.translatable(this.getName() + ".error").withStyle(ChatFormatting.RED));
				}
			}
		}
	}

}
