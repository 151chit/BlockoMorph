package net.blockomorph.utils.dataSyncher;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class IntSyncedData extends AutoSyncedEntityData<Integer> {

	public IntSyncedData(Entity entity, ResourceLocation id, int defaultValue, Runnable onSynced) {
		super(entity, id, defaultValue, onSynced);
	}

	@Override
	protected void readFromBuffer(FriendlyByteBuf buffer) {
		this.data = buffer.readInt();
	}

	@Override
	protected void writeInBuffer(FriendlyByteBuf buffer) {
		buffer.writeInt(this.data);
	}
}
