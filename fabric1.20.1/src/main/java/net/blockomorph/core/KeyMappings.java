package net.blockomorph.core;

import org.lwjgl.glfw.GLFW;

import net.blockomorph.screens.MorphScreen;
import net.blockomorph.screens.BlockMorphConfigScreen;

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
			    mc.setScreen(new MorphScreen(Component.literal("morph_screen")));
			}
		}
	};

	public static final KeyMapping MORPH_CONFIG = new KeyMapping("key.blockomorph.morph_config_menu", GLFW.GLFW_KEY_U, "key.categories.ui") {
		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDown && mc.screen == null) {
			    mc.setScreen(new BlockMorphConfigScreen(Component.literal("morph_config_screen")));
			}
		}
	};

}
