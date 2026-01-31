package net.blockomorph.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public interface BlockMorphPacket {
	void write(FriendlyByteBuf buffer);

	String getId();

	void handle(Player player);
}
