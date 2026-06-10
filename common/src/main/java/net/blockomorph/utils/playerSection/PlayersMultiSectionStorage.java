package net.blockomorph.utils.playerSection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.mixins.main.level.GameEventRegistryAccessor;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import oshi.annotation.concurrent.NotThreadSafe;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

@NotThreadSafe
public class PlayersMultiSectionStorage implements PlayersFinder {
	private final Long2ObjectMap<Int2ObjectMap<PlayerSection>> storage = new Long2ObjectOpenHashMap<>();

	Long2ObjectMap<Int2ObjectMap<PlayerSection>> getMutableStorage() {
		return this.storage;
	}

	public static PlayersFinder fromLevel(EntityGetter level) {
		if (level == null) return (_, _) -> ObjectOpenHashSet.of();
		if (level instanceof PlayersProvider provider) return provider.getStorage$blockomorph();
		return (self, area) -> {
			ObjectOpenHashSet<Player> players = new ObjectOpenHashSet<>(8);
			for (Entity entity : level.getEntities(self, area, EntitySelector.NO_SPECTATORS.and(pl -> pl instanceof Player))) {
				if (entity instanceof Player pl) players.add(pl);
			}
			return players;
		};
	}

	public static ObjectOpenHashSet<BlockInPlayer2> getBlockOnPos(Entity self, Level lv, Vec3 pos) {
		if (lv instanceof PlayersProvider provider) {
			var pls = provider.getStorage$blockomorph().getPlayersOnPos(self, pos, EntitySelector.NO_SPECTATORS);
			ObjectOpenHashSet<BlockInPlayer2> blocks = new ObjectOpenHashSet<>(pls.size());
			for (Player player : pls) {
				if (player instanceof PlayerAccessor pl && pl.isFullActive()) {
					Vec3 realPos = MorphUtils.getRealBlockPos(pl, InPlayerBlockPos.ZERO);
					Vec3 inPlPos = pos.subtract(realPos);
					InPlayerBlockPos blockPos = InPlayerBlockPos.get(Mth.floor(inPlPos.x), Mth.floor(inPlPos.y), Mth.floor(inPlPos.z));
					if (blockPos.isValid()) {
						BlockInPlayer2 block = pl.getBlocksData2().get(blockPos);
 						if (block != null) blocks.add(block);
					}
				}
			}
			return blocks;
		}
		return ObjectOpenHashSet.of();
	}

	public void handleEvent(Vec3 sourcePos, int chunkX, int chunkZ, int sectionY, ListenerVisitorWithHook hookedListener) {
		long chunkPos = ChunkPos.pack(chunkX, chunkZ);
		Int2ObjectMap<PlayerSection> chunk = this.storage.get(chunkPos);
		if (chunk != null) {
			PlayerSection section = chunk.get(sectionY);
			if (section != null) {
				section.iterate(player -> {
					if (player instanceof PlayerAccessor pl && pl.player().level() instanceof ServerLevel lv && hookedListener.alreadyUsed().add(player)) {
						pl.getListenersStorage().iterate(listener -> {
							Optional<Vec3> optional = GameEventRegistryAccessor.handleGameEvent(lv, sourcePos, listener);
							optional.ifPresent(vec3 -> hookedListener.visit(listener, vec3));
						});
					}
				});
			}
		}
	}

	@Override
	public ObjectOpenHashSet<Player> find(Entity self, AABB area) {
		return this.find(self, area, EntitySelector.NO_SPECTATORS);
	}

	public ObjectOpenHashSet<Player> getPlayersOnPos(Entity self, Vec3 pos, Predicate<Entity> test) {
		return this.findInternal(self, plHitbox -> plHitbox.contains(pos), test, pos.x, pos.y, pos.z, pos.x, pos.y, pos.z);
	}

	public ObjectOpenHashSet<Player> find(Entity self, AABB area, Predicate<Entity> test) {
		return this.findInternal(self, plHitbox -> plHitbox.intersects(area), test, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
	}

	public ObjectOpenHashSet<Player> findInternal(Entity self, Predicate<AABB> hitBoxSelector, Predicate<Entity> test,
		double minXraw, double minYraw, double minZraw, double maxXraw, double maxYraw, double maxZraw) {
		int minX = SectionPos.blockToSectionCoord(Math.floor(minXraw));
		int minY = SectionPos.blockToSectionCoord(Math.floor(minYraw));
		int minZ = SectionPos.blockToSectionCoord(Math.floor(minZraw));
		int maxX = SectionPos.blockToSectionCoord(Math.floor(maxXraw));
		int maxY = SectionPos.blockToSectionCoord(Math.floor(maxYraw));
		int maxZ = SectionPos.blockToSectionCoord(Math.floor(maxZraw));
		ObjectOpenHashSet<Player> players = new ObjectOpenHashSet<>(8);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				long chunkPos = ChunkPos.pack(x, z);
				Int2ObjectMap<PlayerSection> chunk = this.storage.get(chunkPos);
				if (chunk != null) {
					for (int y = minY; y <= maxY; y++) {
						PlayerSection section = chunk.get(y);
						if (section != null) for (int i = 0; i < section.getUnsafe().size(); i++) {
							Player player = section.getUnsafe().get(i);
							if (player != self && test.test(player) && hitBoxSelector.test(player.getBoundingBox())) {
								players.add(player);
							}
						}
					}
				}
			}
		}
		return players;
	}

	public record ListenerVisitorWithHook(GameEventListenerRegistry.ListenerVisitor orig, Set<Player> alreadyUsed) implements GameEventListenerRegistry.ListenerVisitor {
		@Override
		public void visit(GameEventListener gameEventListener, Vec3 vec3) {
			this.orig.visit(gameEventListener, vec3);
		}
	}

	static class PlayerSection extends SafeIterableStorage<Player> {
		private final int sectionY;
		private final IntConsumer onEmptyAction;
		PlayerSection(int sectionY, IntConsumer onEmpty) {
			this.sectionY = sectionY;
			this.onEmptyAction = onEmpty;
		}

		private List<Player> getUnsafe() {
			return this.objects;
		}

		@Override
		public void remove(Player player) {
			super.remove(player);
			if (this.objects.isEmpty()) {
				this.onEmptyAction.accept(this.sectionY);
			}
		}
	}
}
