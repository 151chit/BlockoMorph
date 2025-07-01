package net.blockomorph;

import net.blockomorph.network.*;
import net.blockomorph.utils.MorphUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

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
		MorphUtils.registerPacket(ClientBoundBlockPosBoundPacket.ID, ClientBoundBlockPosBoundPacket::new, true);
		MorphUtils.registerPacket(ClientBoundMorphUpdatePacket.ID, ClientBoundMorphUpdatePacket::new, true);
		MorphUtils.registerPacket(ClientBoundServerBlockEntityTagPacket.ID, ClientBoundServerBlockEntityTagPacket::new, true);
		MorphUtils.registerPacket(ClientBoundApplyBlockMorphPacket.ID, ClientBoundApplyBlockMorphPacket::new, true);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);
	}

}
