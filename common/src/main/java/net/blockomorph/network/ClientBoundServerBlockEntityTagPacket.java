package net.blockomorph.network;

import net.blockomorph.screens.morphConfig.nbtEditor.PlayerBlockEntityNbtEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClientBoundServerBlockEntityTagPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_server_block_entity_packet";
	CompoundTag tagForGui;
	boolean message;

	private ClientBoundServerBlockEntityTagPacket(CompoundTag tagForGui, boolean message) {
		this.tagForGui = tagForGui;
		this.message = message;
	}

	public ClientBoundServerBlockEntityTagPacket(FriendlyByteBuf buffer) {
		this.tagForGui = buffer.readNbt();
		this.message = buffer.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(this.tagForGui);
		buffer.writeBoolean(this.message);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (Minecraft.getInstance().screen instanceof PlayerBlockEntityNbtEditor nbtEditor) {
			if (this.message) {
				if (this.tagForGui != null) {
					boolean exc = this.tagForGui.getBooleanOr("exc", true);
					Component text = Component.translatable(exc ? "blockomorph.gui.nbtEditor.parseError.exception" : "blockomorph.gui.nbtEditor.parseError.save");
					String error = text.getString() + this.tagForGui.getString("err");
					nbtEditor.setError(error);
				}
			} else nbtEditor.setNewTag(this.tagForGui);
		}
	}

	public static ClientBoundServerBlockEntityTagPacket createForTag(CompoundTag tag) {
		return new ClientBoundServerBlockEntityTagPacket(tag, false);
	}

	public static ClientBoundServerBlockEntityTagPacket createForError(String error, boolean isException) {
		CompoundTag tag = new CompoundTag();
		tag.putString("err", error);
		tag.putBoolean("exc", isException);
		return new ClientBoundServerBlockEntityTagPacket(tag, true);
	}
}
