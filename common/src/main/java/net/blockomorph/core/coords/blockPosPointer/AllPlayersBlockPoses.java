package net.blockomorph.core.coords.blockPosPointer;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.core.serialization.DataWorker;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AllPlayersBlockPoses {
	private final File file;
	private final Object2LongOpenHashMap<UUID> allPlayers = new Object2LongOpenHashMap<>();

	protected AllPlayersBlockPoses(File file) {
		this.allPlayers.defaultReturnValue(-1);
		this.file = file;
	}

	protected long claimOrGetSectionFor(ServerPlayer player) {
		Side.assertOnGameThread(player);
		long section = this.allPlayers.getLong(player.getUUID());
		if (MorphedPlayerSection.isInvalid(section)) {
			while (MorphedPlayerSection.isInvalid(section) || this.allPlayers.containsValue(section)) {
				section = MorphedPlayerSection.createNew();
			}
			this.allPlayers.put(player.getUUID(), section);
		}
		return section;
	}

	protected void load() {
		if (Side.get() != Side.SERVER) return;
		this.allPlayers.clear();
		try {
			CompoundTag tag = NbtIo.readCompressed(this.file.toPath(), NbtAccounter.unlimitedHeap());
			for (String uuidKey : DataWorker.keysCompound(tag)) {
				if (tag.get(uuidKey) instanceof LongTag value) {
					try {
						UUID uuid = UUID.fromString(uuidKey);
						this.allPlayers.put(uuid, value.getAsLong());
					} catch (Exception e) {
						MorphUtils.LOGGER.error("Cannot read uuid for playerOwner {} to load section", uuidKey);
					}
				} else {
					MorphUtils.LOGGER.error("Section for playerOwner {} is not a long tag", uuidKey);
				}
			}
		} catch (IOException ex) {
			MorphUtils.LOGGER.error("Unable to read players morphed sections ", ex);
		}
	}

	protected void save() {
		if (this.allPlayers.isEmpty() || Side.get() != Side.SERVER) return;
		CompoundTag tag = new CompoundTag();
		this.allPlayers.forEach((UUID uuid, Long section) ->
			tag.putLong(uuid.toString(), section)
		);
		try {
			NbtIo.writeCompressed(tag, this.file.toPath());
		} catch (IOException ex) {
			MorphUtils.LOGGER.error("Unable to write players morphed sections ", ex);
		}
	}
}
