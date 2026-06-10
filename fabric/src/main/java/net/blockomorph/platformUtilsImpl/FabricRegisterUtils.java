package net.blockomorph.platformUtilsImpl;

import com.mojang.brigadier.CommandDispatcher;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FabricRegisterUtils implements RegisterPlatformUtils {
	public static final List<PacketForRegister<?>> PACKETS = new ArrayList<>();

	@Override
	public void registerCommand(TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection> command) {
		CommandRegistrationCallback.EVENT.register(command::accept);
	}

	@Override
	public void addServerStartCallback(Consumer<MinecraftServer> serverCallback) {
		ServerLifecycleEvents.SERVER_STARTING.register(serverCallback::accept);
	}

	@Override
	public <T extends CustomPacketPayload> void registerMainPacket(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
		PayloadTypeRegistry.clientboundPlay().register(type, codec);
		PayloadTypeRegistry.serverboundPlay().register(type, codec);
		ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) -> {
			try {
				handler.accept(packet, new Context(false, context.player()));
			} catch (Throwable e) {
				context.player().connection.disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
			}
		});
		PACKETS.add(new PacketForRegister<>(type, codec, handler));
	}

	public record PacketForRegister<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> coded, BiConsumer<T, Context> handler) {}

	@Override
	public void registerKeyMappings(KeyMapping keyMapping) {
		KeyMappingHelper.registerKeyMapping(keyMapping);
	}
}
