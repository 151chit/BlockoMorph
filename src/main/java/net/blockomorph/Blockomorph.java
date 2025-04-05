package net.blockomorph;

import net.blockomorph.network.*;
import net.blockomorph.utils.MorphUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.HashMap;

@Mod(Blockomorph.MODID)
public class Blockomorph {
	public static final Logger LOGGER = LogManager.getLogger(Blockomorph.class);
	public static final String MODID = "blockomorph";

	public Blockomorph(IEventBus modEventBus) {
		modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
			final PayloadRegistrar registrar = event.registrar(MODID);
			registrar.playBidirectional(MainPacket.ID, MainPacket.STREAM_CODEC, MainPacket::apply);
		});
		MorphUtils.registerPacket(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, true);
		MorphUtils.registerPacket(ServerBoundUseBlockPacket.ID, ServerBoundUseBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundInteractBlockPacket.ID, ServerBoundInteractBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);
	}

}
