package net.blockomorph.core.coords.blockPosPointer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayersBlockPoses {
	private final Long2ObjectOpenHashMap<Player> cache = new Long2ObjectOpenHashMap<>();
	private final Object2LongOpenHashMap<UUID> data = new Object2LongOpenHashMap<>();
	private Player cached;

	protected PlayersBlockPoses() {
		this.data.defaultReturnValue(-1);
	}

	public void clear() {
		this.cache.clear();
		this.data.clear();
		this.cached = null;
	}

	public void put(Player player, long newSection) {
		this.data.put(player.getUUID(), newSection);
		this.cache.put(newSection, player);
		if (this.cached != null && this.cached.getUUID().equals(player.getUUID())) {
			this.cached = player;
		}
	}

	public void remove(UUID uuid) {
		if (this.cached != null && this.cached.getUUID().equals(uuid)) this.cached = null;
		long section = this.data.removeLong(uuid);
		if (section != -1) {
			this.cache.remove(section);
		}
	}

	public Player playerBySection(long section) {
		if (this.cached != null) {
			if (this.validatePlayer(this.cached, section)) return this.cached;
			else this.cached = null;
		}
		Player pl = this.cache.get(section);
		if (!this.validatePlayer(pl, section)) return null;
		this.cached = pl;
		return pl;
	}

	public long sectionByPlayer(UUID uuid) {
		return this.data.getLong(uuid);
	}

	private boolean validatePlayer(Player player, long section) {
		if (player == null || player.isRemoved()) return false;
		InPlayerManager mn = PlayerAccessor.of(player).getManager();
		return mn != null && mn.getSectionId() == section;
	}
}
