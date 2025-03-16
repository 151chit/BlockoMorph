package net.blockomorph.core;

import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import net.blockomorph.network.MainPacket;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.screens.ConfigScreen;
import net.blockomorph.screens.MorphScreen;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Consumer;


public class KeyMappings {
    private static final Minecraft mc = Minecraft.getInstance();
	private static final ArrayList<KeyMapping> KEYS = new ArrayList<>();

	public static final KeyMapping MORPH = new HandlerKeymapping("key.blockomorph.morph_menu", GLFW.GLFW_KEY_Y, () ->
			mc.setScreen(new MorphScreen(Config.Mode.NONE, false))
	);

	public static final KeyMapping MORPH_CONFIG = new HandlerKeymapping("key.blockomorph.morph_config_menu", GLFW.GLFW_KEY_U, () ->
			mc.setScreen(new BlockMorphConfigScreen(false))
	);

	public static final KeyMapping CONFIG = new HandlerKeymapping("key.blockomorph.config_menu", GLFW.GLFW_KEY_N, () -> {
		if (canOpenConfig()) {
			mc.setScreen(new ConfigScreen());
		}
	});

	static void registerKeyMappings(Consumer<KeyMapping> register) {
		for (KeyMapping key : KEYS) {
			register.accept(key);
		}
	}

	private static boolean canOpenConfig() {
		return mc.player != null && mc.player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig");
	}

	private static class HandlerKeymapping extends KeyMapping {
		private final Runnable action;

		public HandlerKeymapping(String lang, int key, Runnable action) {
			super(lang, key, "key.categories.ui");
			KEYS.add(this);
			this.action = action;
		}

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && mc.screen == null) {
				this.action.run();
			}
		}
	}

}
