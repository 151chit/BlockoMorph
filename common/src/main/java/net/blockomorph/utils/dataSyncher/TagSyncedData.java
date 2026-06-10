package net.blockomorph.utils.dataSyncher;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public class TagSyncedData extends AutoSyncedEntityData<CompoundTag> {

	public TagSyncedData(Entity entity, Identifier id, CompoundTag defaultValue, Runnable onSynced) {
		super(entity, id, defaultValue.copy(), onSynced);
	}

	@Override
	protected void readFromBuffer(FriendlyByteBuf buffer) {
		this.data = buffer.readNbt();
	}

	@Override
	protected void writeInBuffer(FriendlyByteBuf buffer) {
		buffer.writeNbt(this.data);
	}
}
