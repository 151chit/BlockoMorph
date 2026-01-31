package net.blockomorph.network;

import net.blockomorph.screens.morphConfig.nbtEditor.PlayerBlockEntityNbtEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;

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
					boolean exc = this.tagForGui.getBooleanOr("exc", false);
					if (!exc) {
						nbtEditor.setError(List.of(Component.translatable("blockomorph.gui.nbtEditor.parseError.save").getString()));
					} else {
						List<String> errors = this.tagForGui.getListOrEmpty("err").stream().map(tag -> {
							if (tag instanceof StringTag(String value)) return value;
							return null;
						}).filter(Objects::nonNull).toList();
						nbtEditor.setError(errors);
					}
				}
			} else nbtEditor.setNewTag(this.tagForGui);
		}
	}

	public static ClientBoundServerBlockEntityTagPacket createForTag(CompoundTag tag) {
		return new ClientBoundServerBlockEntityTagPacket(tag, false);
	}

	public static ClientBoundServerBlockEntityTagPacket createForError(List<String> errors, boolean isException) {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		list.addAll(errors.stream().map(StringTag::valueOf).toList());
		tag.put("err", list);
		tag.putBoolean("exc", isException);
		return new ClientBoundServerBlockEntityTagPacket(tag, true);
	}
}
