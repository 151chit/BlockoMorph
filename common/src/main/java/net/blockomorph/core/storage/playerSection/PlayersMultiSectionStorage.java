package net.blockomorph.core.storage.playerSection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.side.Side;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import oshi.annotation.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;

@NotThreadSafe
public class PlayersMultiSectionStorage implements PlayersStorage {
	private final Long2ObjectMap<Int2ObjectMap<PlayersSection>> storage = new Long2ObjectOpenHashMap<>();
	private final ArrayList<IterationPass> iterStack = new ArrayList<>(16);
	private int iterStackDepth;

	public PlayersMultiSectionStorage() {
		for (int i = 0; i < 16; i ++) {
			this.iterStack.add(new IterationPass());
		}
	}

	Long2ObjectMap<Int2ObjectMap<PlayersSection>> getMutableStorage() {
		return this.storage;
	}

	@Override
	public PlayersMultiSectionStorage findAllPlayers(Entity selfTest, Predicate<PlayerAccessor> morphTest, Predicate<Entity> commonTest, IntersectPredicate boundsTest,
	                                                 double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		Side.assertOnGameThread(selfTest);
		this.iterStackDepth++;
		IterationPass pass;
		if (this.iterStackDepth >= this.iterStack.size()) {
			pass = new IterationPass();
			this.iterStack.add(pass);
		} else pass = this.getCurrentPass();
		pass.self = selfTest; pass.morphTest = morphTest; pass.commonTest = commonTest; pass.boundsTest = boundsTest;
		pass.minX = Math.min(minX, maxX);
		pass.minY = Math.min(minY, maxY);
		pass.minZ = Math.min(minZ, maxZ);
		pass.maxX = Math.max(minX, maxX);
		pass.maxY = Math.max(minY, maxY);
		pass.maxZ = Math.max(minZ, maxZ);
		pass.x = SectionPos.blockToSectionCoord(Mth.floor(pass.minX));
		pass.y = SectionPos.blockToSectionCoord(Mth.floor(pass.minY));
		pass.z = SectionPos.blockToSectionCoord(Mth.floor(pass.minZ));
		return this;
	}

	@Override
	public boolean hasNext() {
		var pass = this.getCurrentPass();
		if (pass.x <= SectionPos.blockToSectionCoord(Mth.floor(pass.maxX)) &&
				pass.y <= SectionPos.blockToSectionCoord(Mth.floor(pass.maxY)) &&
				pass.z <= SectionPos.blockToSectionCoord(Mth.floor(pass.maxZ))) {
			PlayersSection section = pass.getOrActivateSection();
			if (section == null) {
				this.advance();
				return hasNext();
			}
			boolean has = section.hasNext();
			if (has) {
				if (pass.found == null) {
					pass.found = section.next();
					if (!this.testPlayer()) {
						pass.found = null;
						return hasNext();
					}
				}
				return true;
			}
			this.advance();
			return hasNext();
		}
		return false;
	}

	private boolean testPlayer() {
		var pass = this.getCurrentPass();
		Player player = pass.found;
		return player != pass.self && pass.alreadyFound.add(player.getUUID()) && pass.commonTest.test(player) &&
				pass.boundsTest.isValidFor(player.getBoundingBox(), pass.minX, pass.minY, pass.minZ, pass.maxX, pass.maxY, pass.maxZ)
				&& pass.morphTest.test(PlayerAccessor.of(player));
	}

	private void advance() {
		var pass = this.getCurrentPass();
		pass.x++;
		if (pass.x > SectionPos.blockToSectionCoord(Mth.floor(pass.maxX))) {
			pass.x = SectionPos.blockToSectionCoord(Mth.floor(pass.minX));
			pass.y++;
			if (pass.y > SectionPos.blockToSectionCoord(Mth.floor(pass.maxY))) {
				pass.y = SectionPos.blockToSectionCoord(Mth.floor(pass.minY));
				pass.z++;
			}
		}
	}

	@Override
	public Player next() {
		var pass = this.getCurrentPass();
		Player player = pass.found;
		if (player == null) throw new UnsupportedOperationException("Call hasNext first!");
		pass.found = null;
		return player;
	}

	private IterationPass getCurrentPass() {
		if (this.iterStackDepth <= 0) throw new UnsupportedOperationException("Call iterating ticked before!");
		return this.iterStack.get(this.iterStackDepth - 1);
	}

	@Override
	public void close() {
		if (this.iterStackDepth <= 0) throw new IllegalStateException();
		var pass = this.getCurrentPass();
		pass.close();
		this.iterStackDepth--;
	}

	@FunctionalInterface
	public interface IntersectPredicate {
		IntersectPredicate CENTER = (entityHitbox, minX, minY, minZ, maxX, maxY, maxZ) -> {
			double centerX = Mth.lerp(0.5, minX, maxX);
			double centerY = Mth.lerp(0.5, minY, maxY);
			double centerZ = Mth.lerp(0.5, minZ, maxZ);
			return entityHitbox.contains(centerX, centerY, centerZ);
		};
		boolean isValidFor(AABB entityHitbox, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
	}

	private class IterationPass {
		Entity self; Predicate<PlayerAccessor> morphTest; Predicate<Entity> commonTest; IntersectPredicate boundsTest;
		double minX; double minY; double minZ; double maxX; double maxY; double maxZ; int x; int y; int z;
		PlayersSection cachedSection; Player found;
		final ObjectOpenHashSet<UUID> alreadyFound = new ObjectOpenHashSet<>();//temp

		private PlayersSection getOrActivateSection() {
			if (this.cachedSection != null) {
				SectionPos pos = this.cachedSection.getPos();
				if (pos.x() == x && pos.y() == y && pos.z() == z && !this.cachedSection.isEmpty()) return this.cachedSection;
			}
			var chunk = storage.get(ChunkPos.asLong(x, z));
			if (chunk != null) {
				if (this.cachedSection != null) this.cachedSection.afterAccess();
				this.cachedSection = chunk.get(y);
				if (this.cachedSection != null) this.cachedSection.beginAccess();
			}
			return this.cachedSection;
		}

		private void close() {
			if (this.cachedSection != null) this.cachedSection.afterAccess();
			this.found = null; this.cachedSection = null; this.alreadyFound.clear();
			this.self = null;
			this.morphTest = null;
			this.commonTest = null;
			this.boundsTest = null;
		}
	}
}
