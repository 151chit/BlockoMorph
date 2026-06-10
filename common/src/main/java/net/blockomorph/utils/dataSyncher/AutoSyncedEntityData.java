package net.blockomorph.utils.dataSyncher;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public abstract class AutoSyncedEntityData<DATA> {
	protected DATA data;
	protected final Identifier id;
	protected boolean isDirty;
	private final SyncedEntity entity;
	private final Runnable onSynced;

	protected AutoSyncedEntityData(Entity entity, Identifier id, DATA defaultValue, Runnable onSynced) {
		if (entity instanceof SyncedEntity syncedEntity) {
			syncedEntity.registerDataSyncer(this);
			this.id = id;
			this.entity = syncedEntity;
			this.data = Objects.requireNonNull(defaultValue);
			this.onSynced = onSynced;
		} else
			throw new IllegalStateException("Cannot register entity syncer from blockomorph! Most likely mixin not applied or missing! Class: " + entity.getClass() + " Id: " + id);
	}

	public Identifier getId() {
		return this.id;
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public final void writeInNetwork(FriendlyByteBuf buffer) {
		this.writeInBuffer(buffer);
		this.isDirty = false;
	}

	public final void readFromNetwork(FriendlyByteBuf buffer) {
		this.readFromBuffer(buffer);
	}

	protected abstract void readFromBuffer(FriendlyByteBuf buffer);

	protected abstract void writeInBuffer(FriendlyByteBuf buffer);

	public final void onReceived() {
		if (this.onSynced != null) this.onSynced.run();
	}

	public void set(DATA data) {
		if (!this.data.equals(data)) {
			this.data = data;
			this.isDirty = true;
			this.entity.setDirty$blockomorph();
			this.onReceived();
		}
	}

	public DATA get() {
		return this.data;
	}
}
