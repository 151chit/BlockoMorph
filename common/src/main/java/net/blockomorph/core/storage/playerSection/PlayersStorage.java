package net.blockomorph.core.storage.playerSection;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Predicate;

public interface PlayersStorage extends AutoCloseable, Iterator<Player>, Iterable<Player> {
	Predicate<Entity> NOT_MORPHED_PLAYER = entity -> !(entity instanceof PlayerAccessor pl) || !pl.isBlockomorphActive();
	BlockTest ALL_BLOCKS = (ignored, ignored2, ignored3, ignored4, ignored5) -> true;

	PlayersStorage findAllPlayers(Entity selfTest, Predicate<PlayerAccessor> morphTest, Predicate<Entity> commonTest,
	                              PlayersMultiSectionStorage.IntersectPredicate boundsTest,
	                              double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

	default PlayersStorage findMorphedPlayers(Entity selfExclude, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return this.findAllPlayers(selfExclude, PlayerAccessor::isBlockomorphActive,
				EntitySelector.NO_SPECTATORS, AABB::intersects, minX, minY, minZ, maxX, maxY, maxZ);
	}

	default PlayersStorage findMorphedPlayers(Entity self, Vec3 from, Vec3 to) {
		return this.findMorphedPlayers(self, from.x, from.y, from.z, to.x, to.y, to.z);
	}

	default PlayersStorage findMorphedPlayers(Entity self, AABB aabb) {
		return this.findMorphedPlayers(self, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}

	default PlayersStorage findMorphedOnPos(Entity self, double x, double y, double z) {
		return this.findAllPlayers(self, PlayerAccessor::isBlockomorphActive, EntitySelector.NO_SPECTATORS, PlayersMultiSectionStorage.IntersectPredicate.CENTER,
				x - 0.01, y - 0.01, z - 0.01, x + 0.01, y + 0.01, z + 0.01);
	}

	@Nullable
	default BlockInPlayer2 findFirstBlockOnPos(Entity self, double x, double y, double z, BlockTest blockTest) {
		try (this) {
			for (Player player : this.findMorphedOnPos(self, x, y, z)) {
				BlockInPlayer2 block = MorphMath.getBlockInPlayerOnPos(PlayerAccessor.of(player), x, y, z);
				if (block != null && blockTest.isValidFor(block, self, x, y, z)) return block;
			}
		}
		return null;
	}

	static PlayersStorage ofLevel(Object getter) {
		if (getter instanceof Provider p) return p.getStorage();
		return EMPTY;
	}

	@FunctionalInterface
	interface Provider {
		PlayersStorage getStorage();
	}

	@FunctionalInterface
	interface BlockTest {
		boolean isValidFor(BlockInPlayer2 block, Entity self, double x, double y, double z);
	}

	@Override
	default Iterator<Player> iterator() {
		return this;
	}

	@Override
	default Spliterator<Player> spliterator() {
		throw new UnsupportedOperationException("spliterator");
	}

	@Override void close();

	PlayersStorage EMPTY = new PlayersStorage() {
		@Override public PlayersStorage findAllPlayers(Entity selfTest, Predicate<PlayerAccessor> morphTest, Predicate<Entity> commonTest, PlayersMultiSectionStorage.IntersectPredicate boundsTest, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) { return this; }
		@Override public void close() {}
		@Override public boolean hasNext() { return false; }
		@Override public Player next() { throw new NoSuchElementException(); }
	};
}
