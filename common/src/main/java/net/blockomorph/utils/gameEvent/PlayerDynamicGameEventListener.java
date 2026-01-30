package net.blockomorph.utils.gameEvent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

public class PlayerDynamicGameEventListener {
	public final SafeIterableStorage<GameEventListener> storage;
	public final ServerPlayer player;
	private final int[] currentCoords = new int[6];
	private boolean firstRun = true;

	public PlayerDynamicGameEventListener(ServerPlayer player, SafeIterableStorage<GameEventListener> storage) {
		this.storage = storage;
		this.player = player;
	}

	public void onMove() {
		if (this.player.level() instanceof ServerLevelAccessor acc) {
			AABB box = this.player.getBoundingBox();

			int minX = SectionPos.blockToSectionCoord(Math.floor(box.minX));
			int minY = SectionPos.blockToSectionCoord(Math.floor(box.minY));
			int minZ = SectionPos.blockToSectionCoord(Math.floor(box.minZ));
			int maxX = SectionPos.blockToSectionCoord(Math.floor(box.maxX));
			int maxY = SectionPos.blockToSectionCoord(Math.floor(box.maxY));
			int maxZ = SectionPos.blockToSectionCoord(Math.floor(box.maxZ));

			if (this.firstRun || this.isChanged(minX, minY, minZ, maxX, maxY, maxZ)) {
				this.firstRun = false;
				Long2ObjectMap<Int2ObjectMap<SectionGameEventListenerRegistry>> map = acc.getPlayerGameEventListenerMap();
				this.onRemove(map);

				this.save(minX, minY, minZ, maxX, maxY, maxZ);

				for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
					for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
						long chunkKey = ChunkPos.asLong(x, z);
						var sectionMap = map.computeIfAbsent(chunkKey, l -> new Int2ObjectOpenHashMap<>());
						for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
							int finalY = y;
							sectionMap.computeIfAbsent(y, l -> new SectionGameEventListenerRegistry(this.player.serverLevel(), finalY, i -> {
								sectionMap.remove(i);
								if (sectionMap.isEmpty()) {
									map.remove(chunkKey);
								}
							})).add(this.storage);
						}
					}
				}
			}
		}
	}

	public void onRemove(Long2ObjectMap<Int2ObjectMap<SectionGameEventListenerRegistry>> map) {
		for (int x = currentCoords[0]; x <= currentCoords[3]; x++) {
			for (int z = currentCoords[2]; z <= currentCoords[5]; z++) {
				long chunkKey = ChunkPos.asLong(x, z);
				var sectionMap = map.get(chunkKey);
				if (sectionMap != null) {
					for (int y = currentCoords[1]; y <= currentCoords[4]; y++) {
						var sectionRegistry = sectionMap.get(y);
						if (sectionRegistry != null) sectionRegistry.remove(this.storage);
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

	public record ListenerVisitorWithHook(GameEventListenerRegistry.ListenerVisitor orig, HashSet<SafeIterableStorage<GameEventListener>> alreadyUsed) implements GameEventListenerRegistry.ListenerVisitor {
		@Override
		public void visit(GameEventListener gameEventListener, Vec3 vec3) {
			this.orig.visit(gameEventListener, vec3);
		}
	}
}
