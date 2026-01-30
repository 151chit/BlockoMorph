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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TriState;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.commons.lang3.function.TriConsumer;

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
	public <T extends CustomPacketPayload> void registerMainPacket(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
		PACKETS.add(new PacketForRegister<>(type, codec, handler));
	}

	public record PacketForRegister<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> coded, BiConsumer<T, Context> handler) {}

	@Override
	public void registerKeyMappings(KeyMapping keyMapping) {
		if (FMLLoader.getDist() == Dist.CLIENT) {
			ClientListeners.KEYS.add(keyMapping);
		} else throw new RuntimeException("Call keymap register on server!");
	}

	@EventBusSubscriber
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

	@EventBusSubscriber(value = Dist.CLIENT)
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
					event.setUseItem(TriState.FALSE);
				}
			}
		}
	}
}
