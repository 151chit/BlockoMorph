package net.blockomorph.utils.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface RegisterPlatformUtils {
	RegisterPlatformUtils INSTANCE = ServiceLoader.load(RegisterPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load register platform-depended utils, mod cannot run!"));

	void registerCommand(TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection> command);
	void addServerStartCallback(Consumer<MinecraftServer> serverCallback);
	<T extends CustomPacketPayload> void registerMainPacket(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Context> handler);
	record Context(boolean client, @Nullable Player player) {}
	void registerKeyMappings(KeyMapping keyMapping);
}
