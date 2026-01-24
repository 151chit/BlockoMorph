package net.blockomorph.network;

import io.netty.buffer.Unpooled;
import net.blockomorph.screens.morph.tabs.AllowedTab;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigInstance;
import net.blockomorph.utils.config.ConfigStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClientBoundConfigUpdatePacket implements BlockMorphPacket {
	public static final String ID = "client_bound_config_update_packet";
	byte[] payload;

	public ClientBoundConfigUpdatePacket(FriendlyByteBuf buf) {
		this.payload = buf.readByteArray();
	}

	public ClientBoundConfigUpdatePacket(ConfigStorage cfg) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			for (ConfigInstance<?> instance : cfg.LINEAR_OPTIONS) {
				instance.writeToNetwork(buffer);
			}
			byte[] payLoad = new byte[buffer.readableBytes()];
			buffer.readBytes(payLoad);
			this.payload = payLoad;
		} finally {
			buffer.release();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByteArray(this.payload);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (!Minecraft.getInstance().isLocalServer()) {
			FriendlyByteBuf tempBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(this.payload));
			try {
				Config.receiveOnClient(tempBuf);
			} finally {
				tempBuf.release();
			}
		}
		AllowedTab.markDirty();
		if (Minecraft.getInstance().screen instanceof ConfigSyncListener gui) {
			gui.onConfigSynced();
		}
	}
}
