package net.blockomorph.platformUtilsImpl;

import com.mojang.brigadier.CommandDispatcher;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.util.TriConsumer;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FabricRegisterUtils implements RegisterPlatformUtils {
	public static final HashMap<Class<?>, PacketForRegister<?>> PACKETS = new HashMap<>();

	@Override
	public void registerCommand(TriConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection> command) {
		CommandRegistrationCallback.EVENT.register(command::accept);
	}

	@Override
	public void addServerStartCallback(Consumer<MinecraftServer> serverCallback) {
		ServerLifecycleEvents.SERVER_STARTING.register(serverCallback::accept);
	}

	@Override
	public <T> void registerMainPacket(Class<T> type, PacketCodingHandler<T> codec, BiConsumer<T, Context> handler) {
		if (!Modifier.isFinal(type.getModifiers())) throw new IllegalArgumentException("Packet must be final: " + type.getName());
		PacketForRegister<T> coder = new PacketForRegister<>(new ResourceLocation(MorphUtils.MODID, type.getSimpleName().toLowerCase(Locale.ROOT)), codec, handler);
		ServerPlayNetworking.registerGlobalReceiver(coder.type(), (server, player, listener, buf, responseSender) -> {
			T packet = codec.decode(buf);
			server.execute(() -> {
				try {
					handler.accept(packet, new Context(false, player));
				} catch (Throwable e) {
					player.connection.disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
				}
			});
		});
		PACKETS.put(type, coder);
	}

	public record PacketForRegister<T>(ResourceLocation type, PacketCodingHandler<T> codec, BiConsumer<T, Context> handler) {}

	@Override
	public void registerKeyMappings(KeyMapping keyMapping) {
		KeyBindingHelper.registerKeyBinding(keyMapping);
	}
}
