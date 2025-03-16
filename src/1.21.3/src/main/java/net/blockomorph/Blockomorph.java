package net.blockomorph;

import net.blockomorph.network.*;
import net.blockomorph.utils.MorphUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.util.Tuple;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

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
