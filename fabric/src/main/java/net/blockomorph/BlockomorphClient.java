package net.blockomorph;

import net.blockomorph.core.ClientRegister;
import net.blockomorph.platformUtilsImpl.FabricRegisterUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class BlockomorphClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientRegister.register();
		FabricRegisterUtils.PACKETS.values().forEach(this::registerPacket);
	}

	private <T> void registerPacket(FabricRegisterUtils.PacketForRegister<T> packetForRegister) {
		ClientPlayNetworking.registerGlobalReceiver(packetForRegister.type(), (client, listener, buf, responseSender) -> {
			T packet = packetForRegister.codec().decode(buf);
			try {
				packetForRegister.handler().accept(packet, new RegisterPlatformUtils.Context(true, client.player));
			} catch (Throwable e) {
				Objects.requireNonNull(client.getConnection())
						.getConnection().disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
			}
		});
	}
}
