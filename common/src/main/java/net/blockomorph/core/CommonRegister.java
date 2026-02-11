package net.blockomorph.core;

import net.blockomorph.command.BlockmorphCommand;
import net.blockomorph.command.BlockmorphconfigCommand;
import net.blockomorph.network.*;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.platform.RegisterPlatformUtils;

public class CommonRegister {

	public static void register() {
		RegisterPlatformUtils.INSTANCE.registerCommand(BlockmorphCommand::register);
		RegisterPlatformUtils.INSTANCE.registerCommand(BlockmorphconfigCommand::register);
		RegisterPlatformUtils.INSTANCE.addServerStartCallback(server -> {
			Config.setServer(server);
			BlockPosBounds.load();
		});
		RegisterPlatformUtils.INSTANCE.registerMainPacket(MainPacket.class, MainPacket.STREAM_CODEC, MainPacket::apply);
		MorphUtils.registerPacket(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, true);
		MorphUtils.registerPacket(ClientBoundBlockPosBoundPacket.ID, ClientBoundBlockPosBoundPacket::new, true);
		MorphUtils.registerPacket(ClientBoundMorphUpdatePacket.ID, ClientBoundMorphUpdatePacket::new, true);
		MorphUtils.registerPacket(ClientBoundServerBlockEntityTagPacket.ID, ClientBoundServerBlockEntityTagPacket::new, true);
		MorphUtils.registerPacket(ClientBoundApplyBlockMorphPacket.ID, ClientBoundApplyBlockMorphPacket::new, true);
		MorphUtils.registerPacket(ClientBoundEntityDataSyncPacket.ID, ClientBoundEntityDataSyncPacket::new, true);
		MorphUtils.registerPacket(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, false);
		MorphUtils.registerPacket(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, false);
		MorphUtils.registerPacket(ServerBoundSelfNbtRequestPacket.ID, ServerBoundSelfNbtRequestPacket::new, false);
	}
}
