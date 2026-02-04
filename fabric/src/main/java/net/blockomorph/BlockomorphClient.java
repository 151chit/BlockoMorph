package net.blockomorph;

import net.blockomorph.core.ClientRegister;
import net.blockomorph.platformUtilsImpl.FabricRegisterUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class BlockomorphClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientRegister.register();
		FabricRegisterUtils.PACKETS.forEach(this::registerPacket);
	}

	private <T extends CustomPacketPayload> void registerPacket(FabricRegisterUtils.PacketForRegister<T> packetForRegister) {
		ClientPlayNetworking.registerGlobalReceiver(packetForRegister.type(), (packet, context) -> {
			try {
				packetForRegister.handler().accept(packet, new RegisterPlatformUtils.Context(true, context.player()));
			} catch (Throwable e) {
				context.player().connection.getConnection().disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
			}
		});
	}
}
