package net.blockomorph.core.coords.blockPosPointer;

import net.blockomorph.core.coords.proxyChunk.ProxyChunksStorage;
import net.blockomorph.network.ClientBoundBlockPosBoundPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;

public class BlockPosBounds {
	private static final PlayersBlockPoses SERVER = new PlayersBlockPoses();
	private static final PlayersBlockPoses CLIENT = new PlayersBlockPoses();
	private static AllPlayersBlockPoses ALL_PLAYERS;

	public static void registerPlayer(ServerPlayer player) {
		long section = SERVER.sectionByPlayer(player.getUUID());
		if (section == -1) section = ALL_PLAYERS.claimOrGetSectionFor(player);
		SERVER.put(player, section);
	}

	public static void unregisterPlayer(ServerPlayer player) {
		ProxyChunksStorage.releasePlayerChunks(player);
		SERVER.remove(player.getUUID());
	}

	public static void load(MinecraftServer sv) {
		if (ALL_PLAYERS == null) {
			File file = sv.getWorldPath(LevelResource.ROOT).resolve("data").resolve("blockomorph.dat").toFile();
			ALL_PLAYERS = new AllPlayersBlockPoses(file);
		}
		ALL_PLAYERS.load();
	}

	public static void releaseServerData() {
		SERVER.clear();
		if (ALL_PLAYERS != null) ALL_PLAYERS.save();
		else MorphUtils.LOGGER.warn("Player sections is not never loaded?!");
		ALL_PLAYERS = null;
	}

	public static void releaseClientData() {
		CLIENT.clear();
	}

	public static Player getPlayerBySection(long section) {
		if (section == -1) return null;
		PlayersBlockPoses data = selectSide();
		return data != null ? data.playerBySection(section) : null;
	}

	public static long getSectionByPlayer(Player player) {
		if (player == null) return -1;
		PlayersBlockPoses data = selectSide();
		return data != null ? data.sectionByPlayer(player.getUUID()) : -1;
	}

	private static PlayersBlockPoses selectSide() {
		return switch (Side.get()) {
			case SERVER -> SERVER;
			case CLIENT -> CLIENT;
			default -> null;
		};
	}

	public static void refreshForRemoteClient(ServerPlayer targetClient, Player sectionOwner, boolean remove) {
		if (sectionOwner == null || targetClient == null) return;
		long section = getSectionByPlayer(sectionOwner);
		targetClient.connection.send(new ClientBoundBlockPosBoundPacket(sectionOwner, (remove || section == -1) ? null : section).toVanillaClientbound());
	}

	public static void handleBlockPosBound(ClientBoundBlockPosBoundPacket packet) {
		Long section = packet.section();
		Player owner = packet.player();
		if (owner != null) {
			if (section != null) {
				CLIENT.put(owner, section);
			} else {
				ProxyChunksStorage.releasePlayerChunks(owner);
				CLIENT.remove(owner.getUUID());
			}
		}
	}
}
