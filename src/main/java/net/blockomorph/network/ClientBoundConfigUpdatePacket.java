package net.blockomorph.network;

import net.blockomorph.screens.MorphScreen;
import net.blockomorph.utils.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClientBoundConfigUpdatePacket implements BlockMorphPacket {
	public static final String ID = "client_bound_config_update_packet";
	Config config;
	public ClientBoundConfigUpdatePacket(FriendlyByteBuf buf) {
		this.config = Config.readFromBufer(buf);
	}

	public ClientBoundConfigUpdatePacket(Config cfg) {
		this.config = cfg;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		this.config.writeInBufer(buffer);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		Config.load(this.config);
		if (Minecraft.getInstance().screen instanceof MorphScreen s) {
			s.updateAllowed();
		}
	}
}
