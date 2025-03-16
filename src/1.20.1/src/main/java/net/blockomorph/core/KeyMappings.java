package net.blockomorph.core;

import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import net.blockomorph.network.DebugPacket;
import net.blockomorph.network.DebugPacket2;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.screens.ConfigScreen;
import net.blockomorph.screens.MorphScreen;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
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

	public static final KeyMapping db = new HandlerKeymapping("key.blockomorph.config_menu2", GLFW.GLFW_KEY_I, () -> {
		MorphUtils.sendServer(new DebugPacket(BlockHitResult.miss(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO)));
	});

	public static final KeyMapping db2 = new HandlerKeymapping("key.blockomorph.config_menu22", GLFW.GLFW_KEY_J, () -> {
		if (Minecraft.getInstance().hitResult instanceof BlockHitResult bl) {
			MorphUtils.sendServer(new DebugPacket(bl));
			DebugPacket.handleAlternative(bl, mc.player);
		}
	});

	public static final KeyMapping db3 = new HandlerKeymapping("key.blockomorph.config_menu223", GLFW.GLFW_KEY_V, () -> {
		MorphUtils.sendServer(new DebugPacket2());
	});

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		for (KeyMapping key : KEYS) {
			event.register(key);
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
