package net.blockomorph.core.storage.playerSection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.core.InPlayerManager;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class PlayerSectionHandler {
	public final InPlayerManager manager;
	private final int[] currentCoords = new int[6];
	private Player lastPlayer;
	private Level lastLevel;
	private boolean firstRun = true;

	public PlayerSectionHandler(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
		this.lastPlayer = manager.getOwner().player();
		this.lastLevel = this.manager.level();
	}

	public void onAdd() {
		this.onMove();
	}

	public void onHitboxChange() {
		this.onMove();
	}

	public void onManagerUpdate() {
		this.onMove();
	}

	public void onMove() {
		AABB box = this.manager.getOwner().player().getBoundingBox();

		int minX = SectionPos.blockToSectionCoord(Math.floor(box.minX));
		int minY = SectionPos.blockToSectionCoord(Math.floor(box.minY));
		int minZ = SectionPos.blockToSectionCoord(Math.floor(box.minZ));
		int maxX = SectionPos.blockToSectionCoord(Math.floor(box.maxX));
		int maxY = SectionPos.blockToSectionCoord(Math.floor(box.maxY));
		int maxZ = SectionPos.blockToSectionCoord(Math.floor(box.maxZ));
		Level level = this.manager.level();
		Player player = this.manager.getOwner().player();

		if (this.firstRun || this.isChangedBox(minX, minY, minZ, maxX, maxY, maxZ) || level != this.lastLevel || this.lastPlayer != player) {
			this.firstRun = false;
			this.onRemove();

			this.saveBox(minX, minY, minZ, maxX, maxY, maxZ);
			this.lastPlayer = player;
			this.lastLevel = level;

			if (this.lastLevel instanceof PlayersStorage.Provider provider && provider.getStorage() instanceof PlayersMultiSectionStorage st) {
				Long2ObjectMap<Int2ObjectMap<PlayersSection>> map = st.getMutableStorage();

				for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
					for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
						long chunkKey = ChunkPos.asLong(x, z);
						var sectionMap = map.computeIfAbsent(chunkKey, ignored -> new Int2ObjectOpenHashMap<>());
						for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
							SectionPos pos = SectionPos.of(x, y, z);
							sectionMap.computeIfAbsent(y, ignored -> new PlayersSection(pos, i -> {
								sectionMap.remove(i);
								if (sectionMap.isEmpty()) {
									map.remove(chunkKey);
								}
							})).add(this.lastPlayer);
						}
					}
				}
			}
		}
	}

	public void onRemove() {
		if (this.lastLevel instanceof PlayersStorage.Provider provider && provider.getStorage() instanceof PlayersMultiSectionStorage st) {
			Long2ObjectMap<Int2ObjectMap<PlayersSection>> map = st.getMutableStorage();
			for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
				for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
					long chunkKey = ChunkPos.asLong(x, z);
					var sectionMap = map.get(chunkKey);
					if (sectionMap != null) {
						for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
							var sectionRegistry = sectionMap.get(y);
							if (sectionRegistry != null) sectionRegistry.remove(this.lastPlayer.getUUID());
						}
					}
				}
			}
		}
	}

	private boolean isChangedBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return minX != currentCoords[0] || minY != currentCoords[1] || minZ != currentCoords[2] || maxX != currentCoords[3] || maxY != currentCoords[4] || maxZ != currentCoords[5];
	}

	private void saveBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		currentCoords[0] = minX; currentCoords[1] = minY; currentCoords[2] = minZ; currentCoords[3] = maxX; currentCoords[4] = maxY; currentCoords[5] = maxZ;
	}
}
