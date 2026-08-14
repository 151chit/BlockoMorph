package net.blockomorph.core.serialization;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickPriority;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class PlayerLoader implements DataWorker {
	private final PlayerWorldSerializer serializer;
	private volatile boolean working;
	private volatile boolean done;
	private boolean readyForTick;
	private boolean readyForLoadBlock;
	private int paletteReadIndex;
	private BlockState[] palette;
	private int[] poses;
	private int[] types;
	private CompoundTag blockEntityTags;
	private CompoundTag tntTag;
	private final MinecraftServer mainExecutor;

	protected PlayerLoader(PlayerWorldSerializer serializer) {
		this.serializer = serializer;
		this.mainExecutor = serializer.manager.level().getServer();
	}

	public boolean isReadyForLoadBlock() {
		return this.readyForLoadBlock;
	}

	public boolean isDone() {
		return this.done;
	}

	public void tick() {
		if (this.readyForTick && !this.done) {
			for (int i = 0; i < 5000; i++) {
				if (this.paletteReadIndex >= this.poses.length) break;
				this.readAndLoadBlock(this.paletteReadIndex);
				this.paletteReadIndex++;
			}
			if (this.paletteReadIndex >= this.poses.length) {
				this.loadTnt(this.tntTag);
				this.serializer.manager.getHitBoxCalculator().refreshPositions();
				this.serializer.manager.getHitBoxCalculator().enqueueHitboxUpdate();
				this.done = true;
			}
		}
	}

	private void readAndLoadBlock(int listIndex) {
		int type = this.types[listIndex];
		if (type >= this.palette.length) {
			MorphUtils.LOGGER.error("Failed to load block with irregular palette type: {} palette size: {} in player: {}", type,
					this.palette.length, this.serializer.playerId);
			return;
		}
		int intPos = this.poses[listIndex];
		InPlayerBlockPos pos = InPlayerBlockPos.decode(intPos);
		if (pos == null || !pos.isValid()) {
			MorphUtils.LOGGER.error("Failed to load block with irregular pos: {} in player: {}", intPos, this.serializer.playerId);
			return;
		}
		BlockState state = this.palette[type];
		if (state.is(Blocks.AIR)) {
			MorphUtils.LOGGER.error("Failed to load empty block with pos: {} in player: {}", pos, this.serializer.playerId);
			return;
		}
		CompoundTag beTag = this.blockEntityTags.getCompound(intPos + "").orElse(null);
		BlockEntityType<?> beType = null;
		if (beTag != null) {
			String id = beTag.getString("id").orElse(null);
			if (id != null) {
				var key = ResourceLocation.tryParse(id);
				beType = key != null ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(key) : null;
			}
		}
		this.readyForLoadBlock = true;
		BlockInPlayer2 block = this.serializer.manager.directLoadOrOverrideBlock(this, pos, state, beType);
		this.readyForLoadBlock = false;
		if (beTag != null) beTag.getCompound("data").ifPresent(block::loadFullTag);
		this.serializer.manager.getNetworkManager().enqueueBlockNetworkUpdate(block.getOffset().asInt());
	}

	public void run() {
		if (this.working) return;
		this.working = true;
		this.serializer.manager.getFlags().serializingProcess.setValue(true);
		var path = this.serializer.rootPath;
		var uuid = this.serializer.playerId.toString();
		var registryBl = this.serializer.manager.level().holderLookup(Registries.BLOCK);
		var registryFl = this.serializer.manager.level().holderLookup(Registries.FLUID);
		Util.ioPool().execute(() -> this.runLoadTask(path, uuid, registryBl, registryFl));
	}

	private boolean runLoad(CompoundTag blockStates, CompoundTag blockEntityTags, LevelChunkTicks<Block>[] blockTicks, LevelChunkTicks<Fluid>[] fluidTicks, long lifetime, CompoundTag tntTag) {
		ListTag palette = blockStates.getList("palette").orElse(null);
		int[] poses = blockStates.getIntArray("poses").orElse(null);
		int[] types = blockStates.getIntArray("types").orElse(null);
		if (palette == null || poses == null || types == null) return false;
		if (palette.isEmpty() || poses.length == 0 || types.length == 0) return false;
		if (poses.length != types.length) return false;
		BlockState[] palletArr = new BlockState[palette.size()];
		var reg = this.serializer.manager.level().holderLookup(Registries.BLOCK);
		for (int i = 0; i < palette.size(); i++) {
			CompoundTag block = palette.getCompound(i).orElse(null);
			if (block == null) return false;
			palletArr[i] = NbtUtils.readBlockState(reg, block);
		}
		this.readyForTick = true;
		this.palette = palletArr;
		this.poses = poses;
		this.types = types;
		this.tntTag = tntTag;
		this.serializer.manager.changeLifeTime(lifetime);
		this.serializer.manager.getBlockTicks().load(blockTicks);
		this.serializer.manager.getFluidTicks().load(fluidTicks);
		this.blockEntityTags = blockEntityTags;
		return true;
	}

	private void runLoadTask(Path path, String uuid, HolderLookup<Block> blockRegistry, HolderLookup<Fluid> fluidRegistry) {
		CompoundTag morphTag = this.readMorphFile(path, uuid, ".dat").or(() -> this.readMorphFile(path, uuid, ".dat_old")).orElse(null);
		if (morphTag != null && !morphTag.isEmpty()) {
			long lifetime = morphTag.getLongOr("lifetime", 0L);
			CompoundTag ticks = morphTag.getCompound(PlayerWorldSerializer.TICK_TAG).orElse(null);
			LevelChunkTicks<Block>[] blockTicks = this.readTicks(uuid, ticks != null ?
					ticks.getList("blocks").orElse(null) : null, blockRegistry, Registries.BLOCK);
			LevelChunkTicks<Fluid>[] fluidTicks = this.readTicks(uuid, ticks != null ?
					ticks.getList("fluids").orElse(null) : null, fluidRegistry, Registries.FLUID);
			CompoundTag blockStates = morphTag.getCompoundOrEmpty(PlayerWorldSerializer.STATE_TAG);
			CompoundTag blockEntityTags = morphTag.getCompoundOrEmpty(PlayerWorldSerializer.BE_TAG);
			CompoundTag tntTag = morphTag.getCompound("tnt").orElse(null);
			this.mainExecutor.execute(() -> {
				if (!this.runLoad(blockStates, blockEntityTags, blockTicks, fluidTicks, lifetime, tntTag)) {
					this.terminateEmptyOrFailed();
				}
			});
		} else this.mainExecutor.execute(this::terminateEmptyOrFailed);
	}

	private void terminateEmptyOrFailed() {
		this.done = true;
		LevelChunkTicks<Block>[] blockTicks = new LevelChunkTicks[4];
		LevelChunkTicks<Fluid>[] fluidTicks = new LevelChunkTicks[4];
		for (int i = 0; i < 4; i++) {
			blockTicks[i] = new LevelChunkTicks<>();
			fluidTicks[i] = new LevelChunkTicks<>();
		}
		this.serializer.manager.getBlockTicks().load(blockTicks);
		this.serializer.manager.getFluidTicks().load(fluidTicks);
	}

	private <T> LevelChunkTicks<T>[] readTicks(String uuid, ListTag ticks, HolderLookup<T> registry, ResourceKey<? extends Registry<T>> registryKey) {
		LevelChunkTicks<T>[] ticksBoxes = new LevelChunkTicks[4];
		for (int i = 0; i < 4; i++) {
			ListTag boxTag;
			if (ticks != null && (boxTag = ticks.getList(i).orElse(null)) != null) {
				List<SavedTick<T>> savedTicks = new ArrayList<>();
				for (int n = 0; n < boxTag.size(); n++) {
					CompoundTag tg = boxTag.getCompound(n).orElse(null);
					if (tg != null) {
						T type = this.readElement(registry, registryKey, tg.getString("i").orElse(null));
						Long pos = tg.getLong("p").orElse(null);
						Integer delay = tg.getInt("t").orElse(null);
						Integer priority = tg.getInt("q").orElse(null);
						if (type != null && pos != null && delay != null && priority != null) {
							BlockPos blockPos = BlockPos.of(pos);
							if (InPlayerBlockPos.isValid(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
								savedTicks.add(new SavedTick<>(type, blockPos, delay, TickPriority.byValue(priority)));
							} else {
								MorphUtils.LOGGER.error("Failed to load tick for block: {} on irregular pos: {} for player: {}", type, blockPos.toShortString(), uuid);
							}
						}
					}
				}
				ticksBoxes[i] = new LevelChunkTicks<>(savedTicks);
			} else ticksBoxes[i] = new LevelChunkTicks<>();
		}
		return ticksBoxes;
	}

	private <T> T readElement(HolderLookup<T> lookup, ResourceKey<? extends Registry<T>> registry, String el) {
		if (el == null) return null;
		var id = ResourceLocation.tryParse(el);
		if (id == null) return null;
		var wrapper = lookup.get(ResourceKey.create(registry, id));
		if (wrapper.isPresent()) {
			var holder = wrapper.get();
			return holder.value();
		}
		return null;
	}

	private void loadTnt(CompoundTag tntTag) {
		if (tntTag != null) {
			Optional<Entity> entity = EntityType.create(tntTag, this.serializer.manager.level(), EntitySpawnReason.LOAD);
			if (entity.isPresent() && entity.get() instanceof PrimedTnt tnt) {
				tnt.load(tntTag);
				this.serializer.manager.getTntHandler().putTntDirectFromDisk(tnt);
			}
		}
	}

	private Optional<CompoundTag> readMorphFile(Path path, String uuid, String format) {
		File file = new File(path.toFile(), uuid + format);
		if (file.exists() && file.isFile()) {
			try {
				return Optional.of(NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap()));
			} catch (Exception e) {
				MorphUtils.LOGGER.error("Cannot read playerOwner morphData: {} ", uuid, e);
			}
		}
		return Optional.empty();
	}
}
