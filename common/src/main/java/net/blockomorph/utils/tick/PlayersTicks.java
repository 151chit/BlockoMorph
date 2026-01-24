package net.blockomorph.utils.tick;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.SavedTick;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayersTicks<OBJECT> extends LevelTicks<OBJECT> {
	private boolean writeMode;

	private void checkWriteMode() {
		if (!this.writeMode) throw new IllegalStateException("Attempt to bypass player ticks system!");
	}

	@Override
	public void addContainer(ChunkPos chunkPos, LevelChunkTicks<OBJECT> levelChunkTicks) {
		this.checkWriteMode();
		super.addContainer(chunkPos, levelChunkTicks);
	}

	@Override
	public void removeContainer(ChunkPos chunkPos) {
		this.checkWriteMode();
		super.removeContainer(chunkPos);
	}

	@Override
	public void clearArea(BoundingBox boundingBox) {
		this.checkWriteMode();
		super.clearArea(boundingBox);
	}

	@Override
	public void copyAreaFrom(LevelTicks<OBJECT> levelTicks, BoundingBox boundingBox, Vec3i vec3i) {
		this.checkWriteMode();
		super.copyAreaFrom(levelTicks, boundingBox, vec3i);
	}



	private final HashMap<UUID, HashMap<ChunkPos, LevelChunkTicks<OBJECT>>> tickers = new HashMap<>();
	public final String name;
	public final Supplier<Codec<OBJECT>> codecSupplier;

	public PlayersTicks(String name, Supplier<Codec<OBJECT>> codecSupplier) {
		super(l -> true, () -> {
			return Config.getServer() != null ? Config.getServer().getProfiler() : InactiveProfiler.INSTANCE;
		});
		this.name = name;
		this.codecSupplier = codecSupplier;
	}

	public void replaceOrRegisterForPlayer(ServerPlayer player, @Nullable HashMap<ChunkPos, List<SavedTick<OBJECT>>> ticks, long gameTime) {
		ChunkPos[] poses = PlayerMorphedSection.getPlayerChunks(player);
		Long2LongMap absoluteToOffset = PlayerMorphedSection.absoluteToOffset(PlayerAccessor.of(player));
		if (poses != null && absoluteToOffset != null) {
			HashMap<ChunkPos, LevelChunkTicks<OBJECT>> tickMap = this.erase(player);
			this.writeMode = true;
			for (ChunkPos pos : poses) {
				LevelChunkTicks<OBJECT> ticker = ticks != null ? new LevelChunkTicks<>(ticks.get(new ChunkPos(absoluteToOffset.get(pos.toLong())))) : new LevelChunkTicks<>();
				tickMap.put(pos, ticker);
				ticker.unpack(gameTime);
				this.addContainer(pos, ticker);
			}
			this.writeMode = false;
		}
	}

	protected HashMap<ChunkPos, LevelChunkTicks<OBJECT>> erase(ServerPlayer player) {
		HashMap<ChunkPos, LevelChunkTicks<OBJECT>> tickMap = this.tickers.computeIfAbsent(player.getUUID(), c -> new HashMap<>());
		if (!tickMap.isEmpty()) {
			this.writeMode = true;
			for (ChunkPos chunkPos : tickMap.keySet()) {
				BoundingBox box = new BoundingBox(chunkPos.getMinBlockX(), InPlayerBlockPos.Y_CHUNK_START, chunkPos.getMinBlockZ(),
						chunkPos.getMaxBlockX(), InPlayerBlockPos.Y_CHUNK_START + PlayerMorphedSection.MAX_SIZE, chunkPos.getMaxBlockZ());
				this.clearArea(box);
				this.removeContainer(chunkPos);
			}
			tickMap.clear();
			this.writeMode = false;
		}
		return tickMap;
	}

	public void stopAndUnload(ServerPlayer player) {
		this.erase(player);
		this.tickers.remove(player.getUUID());
	}

	@Nullable
	public HashMap<ChunkPos, LevelChunkTicks<OBJECT>> getTickersForPlayer(ServerPlayer player) {
		return this.tickers.get(player.getUUID());
	}
}
