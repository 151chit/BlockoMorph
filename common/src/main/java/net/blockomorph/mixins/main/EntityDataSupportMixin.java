package net.blockomorph.mixins.main;

import net.blockomorph.network.ClientBoundEntityDataSyncPacket;
import net.blockomorph.utils.dataSyncher.AutoSyncedEntityData;
import net.blockomorph.utils.dataSyncher.SyncedEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = Entity.class, priority = 1030)
public abstract class EntityDataSupportMixin implements SyncedEntity {

	@Shadow private int id;
	@Unique private List<AutoSyncedEntityData<?>> SYNCERS;
	@Unique private boolean dirty$blockomorph;

	public void registerDataSyncer(AutoSyncedEntityData<?> data) {
		if (this.getDataById(data.getId()) != null)
			throw new IllegalArgumentException("Data with id: " + this.id + " already registered!");
		this.get().add(data);
	}

	public void setDirty$blockomorph() {
		this.dirty$blockomorph = true;
	}

	public void checkOrSendImmediate(Consumer<ClientBoundEntityDataSyncPacket> doing, boolean force) {
		if (!this.dirty$blockomorph && !force) return;
		for (AutoSyncedEntityData<?> data : this.get()) {
			if (data.isDirty() || force) {
				doing.accept(new ClientBoundEntityDataSyncPacket(this, data));
			}
		}
		this.dirty$blockomorph = false;
	}

	@Nullable
	public AutoSyncedEntityData<?> getDataById(ResourceLocation id) {
		for (AutoSyncedEntityData<?> data : this.get()) {
			if (data.getId().equals(id)) return data;
		}
		return null;
	}

	@Unique
	private List<AutoSyncedEntityData<?>> get() {
		if (SYNCERS == null) {
			SYNCERS = new ArrayList<>();
		}
		return SYNCERS;
	}
}
