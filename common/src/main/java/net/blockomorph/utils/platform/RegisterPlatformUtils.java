package net.blockomorph.utils.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RegisterPlatformUtils {
	RegisterPlatformUtils INSTANCE = ServiceLoader.load(RegisterPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load register platform-depended utils, mod cannot run!"));

	void registerCommand(TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection> command);
	void addServerStartCallback(Consumer<MinecraftServer> serverCallback);
	<T> void registerMainPacket(Class<T> type, PacketCodingHandler<T> codec, BiConsumer<T, Context> handler);
	record Context(boolean client, @Nullable Player player) {}
	void registerKeyMappings(KeyMapping keyMapping);

	interface PacketCodingHandler<PAYLOAD> {
		void encode(FriendlyByteBuf buffer, PAYLOAD payload);
		PAYLOAD decode(FriendlyByteBuf buffer);

		static <T> PacketCodingHandler<T> of(BiConsumer<FriendlyByteBuf, T> encoder, Function<FriendlyByteBuf, T> decoder) {
			return new PacketCodingHandler<>() {
				@Override public void encode(FriendlyByteBuf buffer, T t) { encoder.accept(buffer, t); }
				@Override public T decode(FriendlyByteBuf buffer) { return decoder.apply(buffer); }
			};
		}
	}
}
