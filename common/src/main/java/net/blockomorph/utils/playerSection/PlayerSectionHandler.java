package net.blockomorph.utils.playerSection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class PlayerSectionHandler {
	public final Player player;
	private final int[] currentCoords = new int[6];
	private boolean firstRun = true;

	public PlayerSectionHandler(PlayerAccessor player) {
		this.player = player.player();
	}

	public void onAdd() {
		this.onMove();
	}

	public void onHitboxChange() {
		this.onMove();
	}

	public void onMove() {
		if (this.player.level() instanceof PlayersProvider provider) {
			AABB box = this.player.getBoundingBox();

			int minX = SectionPos.blockToSectionCoord(Math.floor(box.minX));
			int minY = SectionPos.blockToSectionCoord(Math.floor(box.minY));
			int minZ = SectionPos.blockToSectionCoord(Math.floor(box.minZ));
			int maxX = SectionPos.blockToSectionCoord(Math.floor(box.maxX));
			int maxY = SectionPos.blockToSectionCoord(Math.floor(box.maxY));
			int maxZ = SectionPos.blockToSectionCoord(Math.floor(box.maxZ));

			if (this.firstRun || this.isChanged(minX, minY, minZ, maxX, maxY, maxZ)) {
				this.firstRun = false;
				Long2ObjectMap<Int2ObjectMap<PlayersMultiSectionStorage.PlayerSection>> map = provider.getStorage$blockomorph().getMutableStorage();
				this.onRemove();

				this.save(minX, minY, minZ, maxX, maxY, maxZ);

				for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
					for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
						long chunkKey = ChunkPos.pack(x, z);
						var sectionMap = map.computeIfAbsent(chunkKey, l -> new Int2ObjectOpenHashMap<>());
						for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
							int finalY = y;
							sectionMap.computeIfAbsent(y, l -> new PlayersMultiSectionStorage.PlayerSection(finalY, i -> {
								sectionMap.remove(i);
								if (sectionMap.isEmpty()) {
									map.remove(chunkKey);
								}
							})).add(this.player);
						}
					}
				}
			}
		}
	}

	public void onRemove() {
		if (this.player.level() instanceof PlayersProvider provider) {
			Long2ObjectMap<Int2ObjectMap<PlayersMultiSectionStorage.PlayerSection>> map = provider.getStorage$blockomorph().getMutableStorage();
			for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
				for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
					long chunkKey = ChunkPos.pack(x, z);
					var sectionMap = map.get(chunkKey);
					if (sectionMap != null) {
						for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
							var sectionRegistry = sectionMap.get(y);
							if (sectionRegistry != null) sectionRegistry.remove(this.player);
						}
					}
				}
			}
		}
	}

	private boolean isChanged(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return minX != currentCoords[0] || minY != currentCoords[1] || minZ != currentCoords[2] || maxX != currentCoords[3] || maxY != currentCoords[4] || maxZ != currentCoords[5];
	}

	private void save(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		currentCoords[0] = minX; currentCoords[1] = minY; currentCoords[2] = minZ; currentCoords[3] = maxX; currentCoords[4] = maxY; currentCoords[5] = maxZ;
	}
}
