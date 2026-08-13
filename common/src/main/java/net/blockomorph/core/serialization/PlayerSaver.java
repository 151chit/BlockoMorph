package net.blockomorph.core.serialization;

import net.blockomorph.core.TntHandler;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.MorphedBlockProblemReporter;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.SavedTick;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public class PlayerSaver implements DataWorker {
	private final PlayerWorldSerializer serializer;
	private List<SavedTick<Block>>[] blockTicks;
	private List<SavedTick<Fluid>>[] fluidTicks;
	private CompoundTag blocks;
	private final MorphedBlockProblemReporter reporter = new MorphedBlockProblemReporter(20, 500);
	private ArrayList<BlockEntity> blockEntities;
	private final BlockPos zeroPos;
	private long lifetime;
	private int beIndex;
	private final CompoundTag blockEntityTags = new CompoundTag();
	private CompoundTag tntTag;
	private boolean delayTicksSave;
	private volatile boolean asyncWork;
	private volatile CountDownLatch asyncLocker;
	private volatile boolean done;
	private boolean working;

	//read playerOwner data only, no mutate
	protected PlayerSaver(PlayerWorldSerializer serializer) {
		this.serializer = serializer;
		this.zeroPos = serializer.manager.getZeroKey();
	}

	public boolean isDone() {
		return this.done;
	}

	public boolean isAsyncWork() {
		return this.asyncWork;
	}

	public void tick() {
		if (this.done || this.asyncWork) return;
		if (this.delayTicksSave) {
			this.delayTicksSave = false;
			this.saveTicksRaw();
		}
		this.saveBlockentityPerTick();
		if (this.beIndex >= this.blockEntities.size()) {
			this.asyncWork = true;
			this.asyncLocker = new CountDownLatch(1);

			var blocks = this.blocks;
			var blockEntities = this.blockEntityTags;
			var blockTicks = this.blockTicks;
			var fluidTicks = this.fluidTicks;
			var path = this.serializer.rootPath;
			var uuid = this.serializer.playerId.toString();
			var lifetime = this.lifetime;
			var tntTag = this.tntTag;
			Util.ioPool().execute(() -> this.runSaveTask(blocks, blockEntities, blockTicks, fluidTicks, lifetime, tntTag, path, uuid));
		}
	}

	public void run() {
		if (this.done || this.asyncWork || this.working) return;
		this.working = true;
		this.serializer.manager.getFlags().serializingProcess.setValue(true);
		if (this.serializer.manager.getBlockTicks().isTicking() || this.serializer.manager.getFluidTicks().isTicking()) {
			this.delayTicksSave = true;
		} else this.saveTicksRaw();

		this.saveBlocksInTag_andBeRaw();
		this.saveTnt();
	}

	protected void saveAllImmediate() {
		if (this.done) return;
		if (!this.asyncWork) {
			if (this.blockTicks == null || this.fluidTicks == null) this.saveTicksRaw();
			if (this.blocks == null || this.blockEntities == null) this.saveBlocksInTag_andBeRaw();
			if (this.tntTag == null) this.saveTnt();
			while (this.beIndex < this.blockEntities.size()) {
				this.saveBlockentityPerTick();
			}
			this.runSaveTask(this.blocks, this.blockEntityTags, this.blockTicks, this.fluidTicks, this.lifetime, this.tntTag, this.serializer.rootPath, this.serializer.playerId.toString());
		} else {
			try {
				this.asyncLocker.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				MorphUtils.LOGGER.error("Main thread interrupted?");
			}
		}
	}

	private void saveTicksRaw() {
		this.blockTicks = this.serializer.manager.getBlockTicks().save();
		this.fluidTicks = this.serializer.manager.getFluidTicks().save();
		this.lifetime = this.serializer.manager.getLifetime();
	}

	private void saveTnt() {
		TntHandler tntHandler = this.serializer.manager.getTntHandler();
		PrimedTnt tnt = tntHandler.getActiveTnt();
		if (tnt != null) {
			TagValueOutput output = TagValueOutput.createWithContext(this.reporter, this.serializer.manager.level().registryAccess());
			tnt.save(output);
			this.tntTag = output.buildResult();
			this.reporter.logRaw("save tnt data from playerOwner: " + this.serializer.playerId);
		}
	}

	private void saveBlocksInTag_andBeRaw() {
		ArrayList<BlockEntity> blockEntities = new ArrayList<>();
		BlockPalette palette = BlockPalette.packAll(this.serializer.manager.getBlocksStorage(), block -> {
			if (block.getBlockEntity() != null)
				blockEntities.add(block.getBlockEntity());
		});
		this.blocks = palette.toNbt();
		this.blockEntities = blockEntities;
	}

	private void saveBlockentityPerTick() {
		if (this.blockEntities != null) {
			for (int i = 0; i < 500; i++) {
				if (this.beIndex >= this.blockEntities.size()) return;
				BlockEntity blockEntity = this.blockEntities.get(this.beIndex);
				this.blockEntityTags.put(InPlayerBlockPos.fromDelta(this.zeroPos, blockEntity.getBlockPos()) + "", this.saveBE(blockEntity));
				this.beIndex++;
			}
		}
	}

	private CompoundTag saveBE(BlockEntity blockEntity) {
		TagValueOutput output = TagValueOutput.createWithContext(this.reporter, this.serializer.manager.level().registryAccess());
		blockEntity.saveWithoutMetadata(output);
		this.reporter.logRaw("save blockentity for playerOwner " + this.serializer.playerId + " in: " +
				blockEntity.getBlockPos().subtract(this.serializer.manager.getZeroKey()).toShortString());

		CompoundTag blockEntityRoot = new CompoundTag();
		ResourceLocation blockEntityTypeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
		if (blockEntityTypeId != null)
			blockEntityRoot.put("id", StringTag.valueOf(blockEntityTypeId.toString()));
		blockEntityRoot.put("data", output.buildResult());
		return blockEntityRoot;
	}

	private void runSaveTask(CompoundTag blocks, CompoundTag blockEntityTags, List<SavedTick<Block>>[] blockTicks, List<SavedTick<Fluid>>[] fluidTicks, long lifetime, CompoundTag tntTag, Path path, String uuid) {
		try {
			CompoundTag main = new CompoundTag();
			main.putLong("lifetime", lifetime);
			main.put(PlayerWorldSerializer.STATE_TAG, blocks);
			main.put(PlayerWorldSerializer.BE_TAG, blockEntityTags);
			if (tntTag != null) main.put("tnt", tntTag);
			CompoundTag ticks = new CompoundTag();
			ticks.put("blocks", this.serializeTick(blockTicks, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
			ticks.put("fluids", this.serializeTick(fluidTicks, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()));
			main.put(PlayerWorldSerializer.TICK_TAG, ticks);

			Path tmpFile = Files.createTempFile(path, uuid + "-", ".dat");
			NbtIo.writeCompressed(main, tmpFile);
			Path realFile = path.resolve(uuid + ".dat");
			Path oldFile = path.resolve(uuid + ".dat_old");
			Util.safeReplaceFile(realFile, tmpFile, oldFile);
		} catch (Exception e) {
			MorphUtils.LOGGER.error("Cannot write playerOwner morphData: {} ", uuid, e);
		} finally {
			this.done = true;
			this.asyncWork = false;
			if (this.asyncLocker != null)
				this.asyncLocker.countDown();
		}
	}

	private <T> ListTag serializeTick(List<SavedTick<T>>[] ticks, Function<T, String> mapper) {
		ListTag main = new ListTag();
		for (int i = 0; i < 4; i++) {
			ListTag boxTag = new ListTag();
			List<SavedTick<T>> box = ticks[i];
			//noinspection ForLoopReplaceableByForEach
			for (int n = 0; n < box.size(); n++) {
				SavedTick<T> tick = box.get(n);
				CompoundTag tickTag = new CompoundTag();
				tickTag.putString("i", mapper.apply(tick.type()));
				tickTag.putLong("p", tick.pos().asLong());
				tickTag.putInt("t", tick.delay());
				tickTag.putInt("q", tick.priority().getValue());
				boxTag.add(tickTag);
			}
			main.add(boxTag);
		}
		return main;
	}
}
