package net.blockomorph.core;

import net.blockomorph.screens.config.ConfigScreen;
import net.blockomorph.screens.morph.MorphScreen;
import net.blockomorph.screens.morphConfig.MorphConfigScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class KeyMappings {
	private static final Supplier<Minecraft> MC = Minecraft::getInstance;
	private static final ArrayList<KeyMapping> KEYS = new ArrayList<>();

	public static final KeyMapping MORPH = new HandlerKeymapping("morph_menu", GLFW.GLFW_KEY_Y, () -> {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(MC.get().player);
		if (access.morph) {
			MC.get().setScreen(new MorphScreen().ignoreInitInput());
			return true;
		}
		return false;
	});

	public static final KeyMapping MORPH_CONFIG = new HandlerKeymapping("morph_config_menu", GLFW.GLFW_KEY_U, () -> {
		ConfigEnums.ScreenAccess access = MorphUtils.getScreenAccess(MC.get().player);
		PlayerAccessor playerAccessor = PlayerAccessor.of(MC.get().player);
		if (access.config && playerAccessor != null) {
			BlockState blockState = playerAccessor.getBlockState(InPlayerBlockPos.ZERO);
			if (BannedBlock.isBannedBlock(blockState, playerAccessor, BannedBlock.Source.NETWORK) != null) {
				return false;
			}
			MC.get().setScreen(new MorphConfigScreen());
			return true;
		}
		return false;
	});

	public static final KeyMapping CONFIG = new HandlerKeymapping("config_menu", GLFW.GLFW_KEY_N, () -> {
		if (MorphUtils.canOpenConfig()) {
			MC.get().setScreen(new ConfigScreen());
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
			super("blockomorph.key." + lang, key, Category.INVENTORY);
			KEYS.add(this);
			this.action = action;
		}

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && MC.get().screen == null) {
				if (!this.action.getAsBoolean() && MC.get().level != null) {
					GuiUtils.pushHotbarMessage(Component.translatable(this.getName() + ".error").withStyle(ChatFormatting.RED));
				}
			}
		}
	}

}
