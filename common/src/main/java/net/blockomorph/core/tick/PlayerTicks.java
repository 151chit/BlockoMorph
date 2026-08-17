package net.blockomorph.core.tick;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.List;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public class PlayerTicks<GAME_OBJECT> extends LevelTicks<GAME_OBJECT> {
	public static final ChunkPos[] ORDER = {new ChunkPos(0, 0), new ChunkPos(0, -1), new ChunkPos(-1, 0), new ChunkPos(-1, -1)};
	private final LevelChunkTicks<GAME_OBJECT>[] ticks = new LevelChunkTicks[4];
	private final InPlayerManager manager;
	private final BiConsumer<BlockInPlayer2, GAME_OBJECT> tickFunction;
	private boolean ticking;

	public PlayerTicks(InPlayerManager manager, BiConsumer<BlockInPlayer2, GAME_OBJECT> tickFunction) {
		super(ignored -> true);
		this.manager = manager.assertOnInit();
		this.tickFunction = tickFunction;
	}

	public boolean isTicking() {
		return this.ticking;
	}

	public void tick() {
		if (!this.manager.isServer()) return;
		this.ticking = true;
		super.tick(this.manager.getLifetime(), BlocksInPlayerStorage.SIZE, (pos, object) -> {
			BlockInPlayer2 blockInPlayer = this.manager.getBlocksStorage().get(InPlayerBlockPos.asInt(pos.getX(), pos.getY(), pos.getZ()));
			if (blockInPlayer != null) this.tickFunction.accept(blockInPlayer, object);
		});
		this.ticking = false;
	}

	public void load(LevelChunkTicks<GAME_OBJECT>[] ticks) {
		if (ticks.length != 4) throw new IllegalArgumentException();
		for (int i = 0; i < 4; i++) {
			super.removeContainer(ORDER[i]);
			LevelChunkTicks<GAME_OBJECT> box = this.ticks[i] = ticks[i];
			super.addContainer(ORDER[i], box);
			box.unpack(this.manager.getLifetime());
		}
	}

	public List<SavedTick<GAME_OBJECT>>[] save() {
		List<SavedTick<GAME_OBJECT>>[] tickList = new List[4];
		for (int i = 0; i < 4; i++) {
			tickList[i] = this.ticks[i].pack(this.manager.getLifetime());
		}
		return tickList;
	}

	@Override
	public void schedule(ScheduledTick<GAME_OBJECT> tick) {
		long worldTime = this.manager.level().getGameTime();
		ScheduledTick<GAME_OBJECT> newTick = new ScheduledTick<>(tick.type(), this.toOffset(tick.pos()),
				tick.triggerTick() - worldTime + this.manager.getLifetime(), tick.priority(), this.manager.nextTick());
		super.schedule(newTick);
	}

	@Override
	public boolean willTickThisTick(BlockPos pos, GAME_OBJECT type) {
		return super.willTickThisTick(this.toOffset(pos), type);
	}

	@Override
	public boolean hasScheduledTick(BlockPos pos, GAME_OBJECT block) {
		return super.hasScheduledTick(this.toOffset(pos), block);
	}

	private BlockPos toOffset(BlockPos keyPos) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(keyPos.getX()))
			throw new IllegalArgumentException("Not morphed pos! " + keyPos);
		if (InPlayerBlockPos.isInvalidPosFor(this.manager, keyPos))
			throw new IllegalArgumentException("Other playerOwner: " + InPlayerBlockPos.findPlayer(keyPos));
		return keyPos.subtract(this.manager.getZeroKey());
	}

	@Override
	public void tick(long currentTick, int maxTicksToProcess, BiConsumer<BlockPos, GAME_OBJECT> output) {
		throw new UnsupportedOperationException("call baseTick(<noargs>) instead");
	}

	@Override
	public void copyAreaFrom(LevelTicks<GAME_OBJECT> source, BoundingBox area, Vec3i offset) {
		throw new UnsupportedOperationException("copy_area");
	}

	@Override
	public void clearArea(BoundingBox area) {
		throw new UnsupportedOperationException("clear_area");
	}

	@Override
	public void addContainer(ChunkPos pos, LevelChunkTicks<GAME_OBJECT> container) {
		throw new UnsupportedOperationException("call load() instead");
	}

	@Override
	public void removeContainer(ChunkPos pos) {
		throw new UnsupportedOperationException("call save() instead");
	}
}
