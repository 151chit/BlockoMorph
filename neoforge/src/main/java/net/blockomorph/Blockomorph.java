package net.blockomorph;

import net.blockomorph.core.ClientRegister;
import net.blockomorph.core.CommonRegister;
import net.blockomorph.platformUtilsImpl.NeoRegisterUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Deprecated(since = "7.0.4")
@Mod(MorphUtils.MODID)
public class Blockomorph {

	public Blockomorph(IEventBus modEventBus) {
		if (FMLEnvironment.dist == Dist.CLIENT) ClientRegister.register();
		CommonRegister.register();
		modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
			PayloadRegistrar registrar = event.registrar(MorphUtils.MODID);
			NeoRegisterUtils.PACKETS.forEach(packetForRegister -> {
				this.registerPacket(packetForRegister, registrar);
			});
		});
	}

	private <T extends CustomPacketPayload> void registerPacket(NeoRegisterUtils.PacketForRegister<T> packetForRegister, PayloadRegistrar registrar) {
		IPayloadHandler<T> handler = (packet, ctx) -> {
			ctx.enqueueWork(() -> {
				packetForRegister.handler().accept(packet, new RegisterPlatformUtils.Context(ctx.flow() == PacketFlow.CLIENTBOUND, ctx.player()));
			}).exceptionally(e -> {
				ctx.listener().disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
				return null;
			});
		};
		registrar.playBidirectional(packetForRegister.type(), packetForRegister.coded(), handler, handler);
	}

}
