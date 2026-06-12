package net.blockomorph.utils.playerSection;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Set;

public interface PlayersFinder {
	ObjectOpenHashSet<Player> find(Entity self, AABB area);

	default Set<PlayerAccessor> findMorphed(Entity self, AABB area) {//fullActive
		var oldCollection = this.find(self, area);
		ObjectSet<PlayerAccessor> result = new ObjectOpenHashSet<>(oldCollection.size());

		for (var player : oldCollection) {
			PlayerAccessor pl = PlayerAccessor.of(player);
			if (pl.isFullActive()) {
				result.add(pl);
			}
		}
		return result;
	}
}
