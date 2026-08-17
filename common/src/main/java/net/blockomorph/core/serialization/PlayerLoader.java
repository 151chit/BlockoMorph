package net.blockomorph.core.serialization;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.serialization.io.BlockEntityAndEntityIO;
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
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class PlayerLoader implements DataWorker {
	private final BlockEntityAndEntityIO loader = new BlockEntityAndEntityIO();
	private final PlayerWorldSerializer serializer;
	private volatile boolean working;
	private volatile boolean done;
	private boolean readyForTick;
	private boolean readyForLoadBlock;
	private int paletteReadIndex;
	private BlockState[] palette;
	private int[] poses;
	private int[] types;
	@Nullable private CompoundTag blockEntityTags;
	@Nullable private CompoundTag tntTag;
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
		CompoundTag beTag = DataWorker.getTagFrom(this.blockEntityTags, intPos + "", CompoundTag.TYPE).orElse(null);
		BlockEntityType<?> beType = null;
		if (beTag != null) {
			StringTag id = DataWorker.getTagFrom(beTag, "id", StringTag.TYPE).orElse(null);
			if (id != null) {
				var key = ResourceLocation.tryParse(id.value());
				beType = key != null ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(key) : null;
			}
		}
		this.readyForLoadBlock = true;
		BlockInPlayer2 block = this.serializer.manager.directLoadOrOverrideBlock(this, pos, state, beType);
		this.readyForLoadBlock = false;
		if (beTag != null && block.getBlockEntity() != null) {
			CompoundTag data = DataWorker.getTagFrom(beTag, "data", CompoundTag.TYPE).orElse(null);
			if (data != null) {
				var result = this.loader.loadInBlockEntity(block.getBlockEntity(), this.serializer.manager.level().registryAccess(), data);
				BlockEntityAndEntityIO.log(result, "load blockentity for playerOwner " + this.serializer.playerId + " in: " + pos);
			}
		}
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
		if (blockStates == null) return false;
		ListTag palette = DataWorker.getTagFrom(blockStates, "palette", ListTag.TYPE).orElse(null);
		int[] poses = DataWorker.getTagFrom(blockStates, "poses", IntArrayTag.TYPE).map(IntArrayTag::getAsIntArray).orElse(null);
		int[] types = DataWorker.getTagFrom(blockStates, "types", IntArrayTag.TYPE).map(IntArrayTag::getAsIntArray).orElse(null);
		if (palette == null || poses == null || types == null) return false;
		if (palette.isEmpty() || poses.length == 0 || types.length == 0) return false;
		if (poses.length != types.length) return false;
		BlockState[] palletArr = new BlockState[palette.size()];
		var reg = this.serializer.manager.level().holderLookup(Registries.BLOCK);
		for (int i = 0; i < palette.size(); i++) {
			CompoundTag block = DataWorker.getTagFrom(palette, i, CompoundTag.TYPE).orElse(null);
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
			long lifetime = DataWorker.getTagFrom(morphTag, "lifetime", LongTag.TYPE).map(LongTag::value).orElse(0L);
			CompoundTag ticks = DataWorker.getTagFrom(morphTag, PlayerWorldSerializer.TICK_TAG, CompoundTag.TYPE).orElse(null);
			LevelChunkTicks<Block>[] blockTicks = this.readTicks(uuid,
					DataWorker.getTagFrom(ticks, "blocks", ListTag.TYPE).orElse(null), blockRegistry, Registries.BLOCK);
			LevelChunkTicks<Fluid>[] fluidTicks = this.readTicks(uuid,
					DataWorker.getTagFrom(ticks, "fluids", ListTag.TYPE).orElse(null), fluidRegistry, Registries.FLUID);
			CompoundTag blockStates = DataWorker.getTagFrom(morphTag, PlayerWorldSerializer.STATE_TAG, CompoundTag.TYPE).orElse(null);
			CompoundTag blockEntityTags = DataWorker.getTagFrom(morphTag, PlayerWorldSerializer.BE_TAG, CompoundTag.TYPE).orElse(null);
			CompoundTag tntTag = DataWorker.getTagFrom(morphTag, "tnt", CompoundTag.TYPE).orElse(null);
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
			ListTag boxTag = DataWorker.getTagFrom(ticks, i, ListTag.TYPE).orElse(null);
			if (boxTag != null) {
				List<SavedTick<T>> savedTicks = new ArrayList<>();
				for (int n = 0; n < boxTag.size(); n++) {
					CompoundTag tg = DataWorker.getTagFrom(boxTag, n, CompoundTag.TYPE).orElse(null);
					if (tg != null) {
						T type = this.readElement(registry, registryKey, DataWorker.getTagFrom(tg, "i", StringTag.TYPE).orElse(null));
						Long pos = DataWorker.getTagFrom(tg, "p", LongTag.TYPE).map(LongTag::value).orElse(null);
						Integer delay = DataWorker.getTagFrom(tg, "t", IntTag.TYPE).map(IntTag::value).orElse(null);
						Integer priority = DataWorker.getTagFrom(tg, "q", IntTag.TYPE).map(IntTag::value).orElse(null);
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

	private <T> T readElement(HolderLookup<T> lookup, ResourceKey<? extends Registry<T>> registry, StringTag el) {
		if (el == null) return null;
		var id = ResourceLocation.tryParse(el.value());
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
			var resultEntity = this.loader.makeEntityFromTag(this.serializer.manager.level(), tntTag);
			BlockEntityAndEntityIO.log(resultEntity.errors(), "load tnt from disk for" + this.serializer.playerId);
			if (resultEntity.result() instanceof PrimedTnt tnt) {
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
