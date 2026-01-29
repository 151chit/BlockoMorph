package net.blockomorph.utils.dataSyncher;

import net.blockomorph.network.ClientBoundEntityDataSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public interface SyncedEntity {

	void registerDataSyncer(AutoSyncedEntityData<?> data);

	AutoSyncedEntityData<?> getDataById(ResourceLocation id);

	void checkOrSendImmediate(Consumer<ClientBoundEntityDataSyncPacket> doing, boolean force);

	void setDirty$blockomorph();


	static SyncedEntity of(Entity entity) {
		return (SyncedEntity) entity;
	}

	default Entity toEntity() {
		return (Entity) this;
	}
}
