package net.blockomorph.network;

import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class ClientBoundBlockPosBoundPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_blockpos_bound_packet";
	public final PlayerMorphedSection pos;
	public final int id;
	public final boolean delete;

	public ClientBoundBlockPosBoundPacket(FriendlyByteBuf buffer) {
		this.pos = new PlayerMorphedSection(buffer.readLong());
		this.id = buffer.readInt();
		this.delete = buffer.readBoolean();
	}

	public ClientBoundBlockPosBoundPacket(PlayerMorphedSection pos, Player player, boolean delete) {
		this.pos = pos;
		this.id = player.getId();
		this.delete = delete;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeLong(this.pos.toLong());
		buffer.writeInt(this.id);
		buffer.writeBoolean(this.delete);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Nullable
	public Player getPlayer() {
		if (Minecraft.getInstance().level.getEntity(this.id) instanceof Player pl) {
			return pl;
		}
		return null;
	}

	@Override
	public void handle(Player player) {
		BlockPosBounds.handleBlockPosBound(this);
	}
}
