package net.blockomorph.network;

import io.netty.buffer.Unpooled;
import net.blockomorph.utils.dataSyncher.AutoSyncedEntityData;
import net.blockomorph.utils.dataSyncher.SyncedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class ClientBoundEntityDataSyncPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_entity_data_sync_packet";
	byte[] payload;
	int entityId;
	Identifier location;


	public ClientBoundEntityDataSyncPacket(FriendlyByteBuf buffer) {
		this.entityId = buffer.readInt();
		this.location = buffer.readIdentifier();
		this.payload = buffer.readByteArray();
	}

	public ClientBoundEntityDataSyncPacket(SyncedEntity entity, AutoSyncedEntityData<?> data) {
		this.entityId = entity.toEntity().getId();
		this.location = data.getId();
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			data.writeInNetwork(buffer);
			byte[] payLoad = new byte[buffer.readableBytes()];
			buffer.readBytes(payLoad);
			this.payload = payLoad;
		} finally {
			buffer.release();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.entityId);
		buffer.writeIdentifier(this.location);
		buffer.writeByteArray(this.payload);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (Minecraft.getInstance().level.getEntity(this.entityId) instanceof SyncedEntity syncedEntity) {
			AutoSyncedEntityData<?> data = syncedEntity.getDataById(this.location);
			FriendlyByteBuf tempBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(this.payload));
			try {
				data.readFromNetwork(tempBuf);
				data.onReceived();
			} finally {
				tempBuf.release();
			}
		}
	}
}
