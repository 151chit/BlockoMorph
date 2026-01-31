package net.blockomorph.network;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ClientBoundMorphUpdatePacket implements BlockMorphPacket {
	public static final String ID = "client_bound_morph_update_packet";

	CompoundTag tag;
	int id;
	boolean first;

	public ClientBoundMorphUpdatePacket(FriendlyByteBuf buf) {
		this.tag = buf.readNbt();
		this.id = buf.readInt();
		this.first = buf.readBoolean();
	}

	public ClientBoundMorphUpdatePacket(PlayerAccessor player, CompoundTag payload, boolean first) {
		this.tag = payload;
		this.id = player.player().getId();
		this.first = first;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(this.tag);
		buffer.writeInt(this.id);
		buffer.writeBoolean(this.first);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		Entity ent = Minecraft.getInstance().level.getEntity(this.id);
		if (ent instanceof PlayerAccessor pl)
			pl.loadBlockData(this.tag, this, this.first);
	}

	public ClientPacketListener getListener() {
		return Minecraft.getInstance().getConnection();
	}
}
