package net.blockomorph.utils.coords;

import net.blockomorph.network.ClientBoundBlockPosBoundPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.SideSelector;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class BlockPosBounds {
	private static File FILE;
	public static final int MAX_PLAYER_CHUNKS = 1_875_000;
	private static final RandomSource random = RandomSource.create();

	public static final HashMap<PlayerMorphedSection, Player> CACHE = new HashMap<>();
	public static final HashMap<PlayerMorphedSection, Player> CLIENT_CACHE = new HashMap<>();

	private static final HashMap<UUID, PlayerMorphedSection> BOUNDS = new HashMap<>();
	private static final HashMap<UUID, PlayerMorphedSection> CLIENT_BOUNDS = new HashMap<>();

	public static void boundPlayer(Player player) {
		if (player instanceof ServerPlayer) {
			UUID uuid = player.getUUID();
			if (!BOUNDS.containsKey(uuid)) {
				PlayerMorphedSection pos = getChunkPos();
				BOUNDS.put(uuid, pos);
				save();
			}
			CACHE.put(BOUNDS.get(uuid), player);
		}
	}

	private static PlayerMorphedSection getChunkPos() {
		while (true) {
			int x = random.nextInt(MAX_PLAYER_CHUNKS);
			int z = random.nextInt(MAX_PLAYER_CHUNKS);
			PlayerMorphedSection pos = new PlayerMorphedSection(x, z);
			if (!BOUNDS.containsValue(pos)) {
				return pos;
			}
		}
	}

	@Nullable
	public static PlayerMorphedSection getChunkPosForPlayer(Player player) {
		if (player.level().isClientSide())
			return CLIENT_BOUNDS.get(player.getUUID());
		return BOUNDS.get(player.getUUID());
	}

	public static PlayerMorphedSection getPlayerSectionPos(BlockPos bounded) {
		int blockZ = bounded.getZ();
		int blockX = bounded.getX();

		int shiftedZ = blockZ;
		int shiftedX = blockX - InPlayerBlockPos.X_CHUNK_START;

		int sectorZ = shiftedZ / PlayerMorphedSection.FOR_TWO_CHUNKS;
		int sectorX = shiftedX / PlayerMorphedSection.FOR_TWO_CHUNKS;

		return new PlayerMorphedSection(sectorX, sectorZ);
	}

	@Nullable
	public static Player getPlayerByChunkPos(PlayerMorphedSection pos, @Nullable Boolean client) {
		HashMap<PlayerMorphedSection, Player> cash;
		if (client != null) {
			cash = client ? CLIENT_CACHE : CACHE;
		} else {
			cash = SideSelector.isClient() ? CLIENT_CACHE : CACHE;
		}
		Player pl = cash.get(pos);
		if (pl == null)
			return null;
		if (pl.isRemoved()) {
			cash.remove(pos);
			pl = null;
		}
		return pl;
	}

	private static void initFile() {
		if (Config.getServer() != null) {
			FILE = Config.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("blockomorph.dat").toFile();
		}
	}

	public static void load() {
		initFile();
		if (FILE == null || !FILE.exists()) return;
		CompoundTag tg;
		try {
			tg = NbtIo.readCompressed(FILE.toPath(), NbtAccounter.unlimitedHeap());
		} catch (IOException ex) {
			MorphUtils.LOGGER.error("Unable to read PLAYER-BLOCKPOS mappings.", ex);
			return;
		}
		for (String key : tg.keySet()) {
			BOUNDS.put(UUID.fromString(key), new PlayerMorphedSection(tg.getLong(key).orElseThrow()));
		}
	}

	private static void save() {
		if (FILE == null) return;
		CompoundTag tg = new CompoundTag();
		BOUNDS.forEach(((uuid, chunkPos) -> {
			tg.putLong(uuid.toString(), chunkPos.toLong());
		}));
		try {
			NbtIo.writeCompressed(tg, FILE.toPath());
		} catch (IOException ex) {
			MorphUtils.LOGGER.error("Unable to save PLAYER-BLOCKPOS mappings.", ex);
		}
	}

	public static void clearServerData() {
		BOUNDS.clear();
		CACHE.clear();
	}

	public static void clearClientCache() {
		CLIENT_BOUNDS.clear();
		CLIENT_CACHE.clear();
	}

	public static void onJoin(Player entity) {
		boundPlayer(entity);
		PlayerMorphedSection pos = getChunkPosForPlayer(entity);
		if (pos != null) {
			MorphUtils.sendPlayer(new ClientBoundBlockPosBoundPacket(pos, entity, false), (ServerPlayer) entity);
		}
	}

	public static void handleBlockPosBound(ClientBoundBlockPosBoundPacket packet) {
		if (packet.delete) {
			Player old = CLIENT_CACHE.remove(packet.pos);
			if (old != null)
				CLIENT_BOUNDS.remove(old.getUUID());
		} else {
			Player pl = packet.getPlayer();
			if (pl != null) {
				CLIENT_BOUNDS.put(pl.getUUID(), packet.pos);
				CLIENT_CACHE.put(packet.pos, pl);
			}
		}
	}

}
