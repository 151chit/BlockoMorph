package net.blockomorph.input;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.screens.config.ConfigScreen;
import net.blockomorph.screens.morph.MorphScreen;
import net.blockomorph.screens.morphConfig.MorphConfigScreen;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.blockomorph.utils.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class KeyBindings {
	private static final List<KeyBind> KEYS = new ArrayList<>();

	private static class KeyBind extends KeyMapping {
		final Predicate<Minecraft> function;
		public KeyBind(String name, int keysym, Predicate<Minecraft> function) {
			super("blockomorph.key." + name, keysym, Category.MISC);
			KEYS.add(this);
			this.function = function;
		}

		void handle() {
			boolean yes = false;
			while (this.consumeClick()) {
				yes = true;
			} if (yes) {
				if (!this.function.test(GuiUtils.MC)) {
					var text = Component.translatable(this.getName() + ".error").withStyle(ChatFormatting.RED);
					GuiUtils.MC.gui.setOverlayMessage(text, false);
					GuiUtils.MC.getNarrator().saySystemNow(text);
				}
			}
		}
	}
	
	public static void register(Consumer<KeyMapping> registrar) {
		KEYS.forEach(registrar);
	}

	public static void handle() {
		KEYS.forEach(KeyBind::handle);
	}

	static {
		new KeyBind("morph_menu", GLFW.GLFW_KEY_Y, client -> {
			if (MorphUtils.getScreenAccess(client.player).morph) {
				client.setScreen(new MorphScreen());
				return true;
			}
			return false;
		});
		new KeyBind("morph_config_menu", GLFW.GLFW_KEY_U, client -> {
			PlayerAccessor pl = PlayerAccessor.of(client.player);
			if (pl != null && MorphUtils.getScreenAccess(client.player).config) {
				if (BannedBlock.isBannedBlock(pl.getBlockState(InPlayerBlockPos.ZERO), pl, BannedBlock.Source.NETWORK) != null) {
					return false;
				}
				client.setScreen(new MorphConfigScreen());
				return true;
			}
			return false;
		});
		new KeyBind("config_menu", GLFW.GLFW_KEY_N, client -> {
			if (client.player != null && MorphUtils.hasBlockMorphActionsPermissions(client.player.permissions(), MorphUtils.AllowType.CONFIG_SCREEN) &&
					Config.get().canOperatorModifyConfig.getValue()) {
				client.setScreen(new ConfigScreen());
				return true;
			}
			return false;
		});
		if (EarlyLoadingPlatformService.INSTANCE.isRunningInIde()) {
			new KeyMapping(MorphUtils.MODID + "_debug", GLFW.GLFW_KEY_K, KeyMapping.Category.DEBUG) {
				@Override
				public void setDown(boolean down) {
					super.setDown(down);
					if (GuiUtils.MC.level != null && down) debug();
				}
			};
		}
	}




	private static void debug() {

	}



}
