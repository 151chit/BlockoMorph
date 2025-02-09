package net.blockomorph.core;

import org.lwjgl.glfw.GLFW;

import net.blockomorph.screens.*;
import net.blockomorph.utils.config.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;


public class KeyMappings {
    private static final Minecraft mc = Minecraft.getInstance();
	public static final KeyMapping MORPH = new KeyMapping("key.blockomorph.morph_menu", GLFW.GLFW_KEY_Y, "key.categories.ui") {
		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && mc.screen == null) {
			    mc.setScreen(new MorphScreen(Config.Mode.NONE, false));
			}
		}
	};

	public static final KeyMapping MORPH_CONFIG = new KeyMapping("key.blockomorph.morph_config_menu", GLFW.GLFW_KEY_U, "key.categories.ui") {
		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && mc.screen == null) {
			    mc.setScreen(new BlockMorphConfigScreen(false));
			}
		}
	};

	public static final KeyMapping CONFIG = new KeyMapping("key.blockomorph.config_menu", GLFW.GLFW_KEY_N, "key.categories.ui") {
		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (canOpenConfig() && isDown && mc.screen == null) {
			    mc.setScreen(new ConfigScreen());
			}
		}
	};

	private static boolean canOpenConfig() {
	    if (Config.getInstance() != null) return mc.player != null && mc.player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig");
	    return false;
	}

	//debug
	//public static final KeyMapping DEBUG = new KeyMapping("key.blockomorph.config_menu", GLFW.GLFW_KEY_I, "key.categories.ui");

}
