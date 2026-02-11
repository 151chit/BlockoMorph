package net.blockomorph.platformUtilsImpl;

import com.mojang.brigadier.CommandDispatcher;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NeoRegisterUtils implements RegisterPlatformUtils {
	private static final List<TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection>> COMMANDS = new ArrayList<>();
	private static final List<Consumer<MinecraftServer>> SERVER_CALLBACKS = new ArrayList<>();
	public static final List<PacketForRegister<?>> PACKETS = new ArrayList<>();

	@Override
	public void registerCommand(TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection> command) {
		COMMANDS.add(command);
	}

	@Override
	public void addServerStartCallback(Consumer<MinecraftServer> serverCallback) {
		SERVER_CALLBACKS.add(serverCallback);
	}

	@Override
	public <T> void registerMainPacket(Class<T> type, PacketCodingHandler<T> codec, BiConsumer<T, Context> handler) {
		PACKETS.add(new PacketForRegister<>(type, codec, handler));
	}

	public record PacketForRegister<T>(Class<T> type, PacketCodingHandler<T> codec, BiConsumer<T, Context> handler) {}

	@Override
	public void registerKeyMappings(KeyMapping keyMapping) {
		if (FMLLoader.getDist() == Dist.CLIENT) {
			ClientListeners.KEYS.add(keyMapping);
		} else throw new RuntimeException("Call keymap register on server!");
	}

	@Mod.EventBusSubscriber
	public static class ServerListeners {
		@SubscribeEvent
		public static void registerCommand(RegisterCommandsEvent event) {
			COMMANDS.forEach(command ->
					command.accept(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
		}

		@SubscribeEvent
		public static void serverRun(ServerStartingEvent event) {
			SERVER_CALLBACKS.forEach(c -> c.accept(event.getServer()));
		}
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT)
	public static class ClientListeners {
		private static final List<KeyMapping> KEYS = new ArrayList<>();
		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event) {
			KEYS.forEach(event::register);
		}

		@SubscribeEvent
		public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
			if (event.getEntity().getItemInHand(event.getHand()).getItem() instanceof BlockItem) {
				ConfigEnums.PlaceMode mode = Config.get().placeMode.getValue();
				if (mode == ConfigEnums.PlaceMode.DISABLED && InPlayerBlockPos.isMorphedPlayerX(event.getHitVec().getBlockPos().getX())) {
					event.setUseItem(Event.Result.DENY);
				}
			}
		}
	}
}
