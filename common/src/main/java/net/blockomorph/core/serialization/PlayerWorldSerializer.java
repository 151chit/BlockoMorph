package net.blockomorph.core.serialization;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.network.ClientBoundSerializeInfoPacket;
import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class PlayerWorldSerializer {
	private static final Object2ObjectOpenHashMap<UUID, PlayerWorldSerializer> STORAGE = new Object2ObjectOpenHashMap<>();
	public static final String SAVE_DIR = "blockomorphs";
	public static final String STATE_TAG = "blockStates";
	public static final String BE_TAG = "blockEntities";
	public static final String TICK_TAG = "ticks";

	public static void freezeAndRunSaveProcess(ServerPlayer player) {
		if (player.isRemoved()) {
			PlayerWorldSerializer serializer = STORAGE.computeIfAbsent(player.getUUID(), ignored ->
					new PlayerWorldSerializer(player, false));
			serializer.startSave();
		}
	}

	public static void loadNewPlayer(ServerPlayer player) {
		PlayerWorldSerializer serializer = STORAGE.computeIfAbsent(player.getUUID(), ignored ->
				new PlayerWorldSerializer(player, true));
		serializer.startLoad(player);
	}

	public static void tickAll() {
		STORAGE.object2ObjectEntrySet().removeIf(entry -> entry.getValue().tick());
	}

	public static void terminateAllImmediate() {
		STORAGE.forEach((ignored, serializer) -> serializer.saveAllImmediate());
		STORAGE.clear();
	}

	public static boolean inSerializeProcess(UUID uuid) {
		return STORAGE.containsKey(uuid);
	}

	protected final InPlayerManager manager;
	protected final UUID playerId;
	protected final Path rootPath;
	private final boolean firstEnter;
	private DataWorker worker;
	private boolean dataDirty;
	private boolean needAdditionalSave;

	private PlayerWorldSerializer(ServerPlayer player, boolean initiatedByFirstEnter) {
		InPlayerManager manager = PlayerAccessor.of(player).getManager();
		if (manager == null)
			throw new IllegalStateException("Player " + player + " have not initialized manager!");
		this.manager = manager;
		this.playerId = player.getUUID();
		this.rootPath = player.level().getServer().getWorldPath(LevelResource.ROOT).resolve(SAVE_DIR);
		this.firstEnter = initiatedByFirstEnter;
		File file = this.rootPath.toFile();
		if (!file.exists() && !file.mkdirs()) {
			MorphUtils.LOGGER.error("Failed to create root playerOwner morphs folder!");
		}
	}

	//before load/save
	public boolean tick() {
		if (this.worker != null) {
			if (!this.worker.isDone()) {
				this.worker.tick();
			} else {
				if (this.needAdditionalSave) {
					this.needAdditionalSave = false;
					this.worker = new PlayerSaver(this);
					this.worker.run();
					return false;
				}
				this.unfreeze();
				return true;
			}
		}
		return false;
	}

	//spawn task after load
	public void startLoad(ServerPlayer player) {
		if (this.firstEnter) {
			if (this.worker == null) this.worker = new PlayerLoader(this);
			this.worker.run();
		} else {
			if (this.worker instanceof PlayerSaver saver && !saver.isAsyncWork()) {
				this.worker = null;
				STORAGE.remove(this.playerId);
			} else {
				this.dataDirty = true;
			}
			this.unfreeze();
		}
		this.manager.changeOwnerDirectTo(player);
	}

	private void unfreeze() {
		this.manager.getFlags().serializingProcess.setValue(false);
		if (this.manager.getOwner() instanceof ServerPlayer pl) {
			MorphNetwork.sendAll(pl.level().getServer(), new ClientBoundSerializeInfoPacket(false, pl));
		}
	}

	//playerlist remove tail
	public void startSave() {
		if (!this.firstEnter) {
			if (this.worker == null) {
				this.worker = new PlayerSaver(this);
			}
			this.worker.run();
			if (this.dataDirty) {
				this.dataDirty = false;
				this.needAdditionalSave = true;
			}
		}
		this.manager.changeOwnerDirectTo(null);
	}

	//server stop
	public void saveAllImmediate() {
		if (this.worker instanceof PlayerSaver saver) {
			saver.saveAllImmediate();
			if (this.needAdditionalSave) {
				new PlayerSaver(this).saveAllImmediate();
			}
		}
	}
}
