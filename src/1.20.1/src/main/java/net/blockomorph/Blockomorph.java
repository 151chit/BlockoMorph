package net.blockomorph;

import net.blockomorph.network.*;
import net.blockomorph.network.blockFix.ClientBoundBlockEventPacket;
import net.blockomorph.network.blockFix.ServerBoundSignUpdatePacket;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Blockomorph.MODID)
public class Blockomorph {
	public static final Logger LOGGER = LogManager.getLogger(Blockomorph.class);
	public static final String MODID = "blockomorph";
	public static final String PROTOCOL_VERSION = "4";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	public Blockomorph() {
		MorphUtils.registerPacket(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, true);
		MorphUtils.registerPacket(ClientBoundBlockEventPacket.ID, ClientBoundBlockEventPacket::new, true);
		MorphUtils.registerPacket(ServerBoundSignUpdatePacket.ID, ServerBoundSignUpdatePacket::new, false);
		MorphUtils.registerPacket(ServerBoundUseBlockPacket.ID, ServerBoundUseBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundInteractBlockPacket.ID, ServerBoundInteractBlockPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);

		//MorphUtils.registerPacket("db", DebugPacket::new, false);
		//MorphUtils.registerPacket("db2", DebugPacket2::new, false);
		PACKET_HANDLER.registerMessage(0, MainPacket.class, MainPacket::write, MainPacket::new, MainPacket::handler);
	}

}
