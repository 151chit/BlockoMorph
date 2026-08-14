package net.blockomorph.core.serialization.dataFixer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.serialization.BlockPalette;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.core.tick.PlayerTicks;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

class FixerV2 extends DataFixerHandler.DataFixer {

	@Override
	String oldTagRootName() {
		return "BlockoMorph";
	}

	@Override
	CompoundTag tryFixRootTag(UUID playerId, RegistryAccess registry, CompoundTag v2) {
		CompoundTag main = new CompoundTag();

		Int2ObjectMap<BlockState> states = new Int2ObjectOpenHashMap<>(BlocksInPlayerStorage.ONE_AXIS);
		CompoundTag rootBeTag = new CompoundTag();
		this.readBlocks(v2, playerId, registry, states, rootBeTag);
		main.put(PlayerWorldSerializer.STATE_TAG, BlockPalette.packRaw(states).toNbt());
		main.put(PlayerWorldSerializer.BE_TAG, rootBeTag);

		CompoundTag ticksRoot = new CompoundTag();
		this.readTicks(v2, playerId, ticksRoot);
		main.put(PlayerWorldSerializer.TICK_TAG, ticksRoot);
		return main;
	}

	private void readBlocks(CompoundTag oldRoot, UUID playerId, RegistryAccess registry, Int2ObjectMap<BlockState> states, CompoundTag rootBeTag) {
		for (String posHolderKey : oldRoot.keySet()) {
			int pos = this.decodeStringPos(posHolderKey);
			if (pos == -1) {
				MorphUtils.LOGGER.error("Block with irregular pos in V2 data: {} for player: {}", posHolderKey, playerId);
				continue;
			}

			CompoundTag blockHolder = oldRoot.getCompound(posHolderKey).orElse(null);
			if (blockHolder == null) {
				MorphUtils.LOGGER.error("Pos without block in V2 data: {} for player: {}", posHolderKey, playerId);
				continue;
			}

			BlockState blockState = NbtUtils.readBlockState(registry.lookupOrThrow(Registries.BLOCK), blockHolder.getCompoundOrEmpty("BlockState"));
			if (blockState.is(Blocks.AIR)) {
				MorphUtils.LOGGER.error("Empty block in V2 data: {} for player: {}", posHolderKey, playerId);
				continue;
			}
			states.put(pos, blockState);

			CompoundTag beTagContainer = blockHolder.getCompound("BlockEntityTag").orElse(null);
			if (beTagContainer != null && !beTagContainer.isEmpty()) {
				CompoundTag beTag = new CompoundTag();
				String id = beTagContainer.getString("id").orElse(null);
				beTagContainer.remove("id");
				beTag.put("data", beTagContainer);
				if (id != null) {
					beTag.putString("id", id);
				}
				rootBeTag.put(pos + "", beTag);
			}
		}
	}

	private void readTicks(CompoundTag oldRoot, UUID playerId, CompoundTag ticksRoot) {
		CompoundTag ticks = oldRoot.getCompound("ticks").orElse(null);
		if (ticks != null) {
			ticks.getCompound("blockTicks").ifPresent(blockHolder -> {
				ListTag tg = new ListTag();
				ticksRoot.put("blocks", tg);
				readTicksTyped(blockHolder, playerId, tg);
			});
			ticks.getCompound("fluidTicks").ifPresent(fluidHolder -> {
				ListTag tg = new ListTag();
				ticksRoot.put("fluids", tg);
				readTicksTyped(fluidHolder, playerId, tg);
			});
		}
	}

	private void readTicksTyped(CompoundTag oldHolder, UUID playerId, ListTag ticksHolder) {
		for (int i = 0; i < PlayerTicks.ORDER.length; i++) {
			ticksHolder.add(new ListTag());
		}
		for (Tag tag : oldHolder.values()) {
			if (!(tag instanceof ListTag chunk)) continue;
			CachedTicksChunk cachedChunk = null;
			for (Tag blockElement : chunk) {
				if (!(blockElement instanceof CompoundTag blockTag)) continue;
				cachedChunk = handleTick(blockTag, playerId, ticksHolder, cachedChunk);
			}
		}
	}

	private CachedTicksChunk handleTick(CompoundTag blockTag, UUID playerId, ListTag ticksHolder, CachedTicksChunk cachedBox) {
		Integer x = blockTag.getInt("x").orElse(null);
		Integer y = blockTag.getInt("y").orElse(null);
		Integer z = blockTag.getInt("z").orElse(null);
		String type = blockTag.getString("i").orElse(null);
		Integer time = blockTag.getInt("t").orElse(null);
		Integer priority = blockTag.getInt("p").orElse(null);
		if (x == null || y == null || z == null || type == null || time == null || priority == null)
			return cachedBox;
		if (!InPlayerBlockPos.isValid(x, y, z)) {
			MorphUtils.LOGGER.error("Tick for block in V2 data: {} with irregular pos: {} for player: {}",
					type, new BlockPos(x, y, z).toShortString(), playerId);
			return cachedBox;
		}
		int chunkX = SectionPos.blockToSectionCoord(x);
		int chunkZ = SectionPos.blockToSectionCoord(z);
		long currentChunkPos = ChunkPos.asLong(chunkX, chunkZ);

		if (cachedBox == null || cachedBox.pos() != currentChunkPos) {
			cachedBox = new CachedTicksChunk(currentChunkPos, findBoxForChunk(chunkX, chunkZ, ticksHolder));
		}

		CompoundTag tickTag = new CompoundTag();
		tickTag.putString("i", type);
		tickTag.putLong("p", BlockPos.asLong(x, y, z));
		tickTag.putInt("t", time);
		tickTag.putInt("q", priority);

		cachedBox.box().add(tickTag);
		return cachedBox;
	}

	private record CachedTicksChunk(long pos, ListTag box) {}

	private ListTag findBoxForChunk(int chunkX, int chunkZ, ListTag ticksHolder) {
		for (int i = 0; i < PlayerTicks.ORDER.length; i++) {
			ChunkPos orderPos = PlayerTicks.ORDER[i];
			if (orderPos.x == chunkX && orderPos.z == chunkZ) {
				return ticksHolder.getList(i).orElseThrow();
			}
		}
		throw new IllegalStateException("Irregular chunk pos: " + new ChunkPos(chunkX, chunkZ));
	}

	private int decodeStringPos(String key) {
		try {
			String[] parts = key.split(" ");
			if (parts.length == 3) {
				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				int z = Integer.parseInt(parts[2]);
				if (!InPlayerBlockPos.isValid(x, y, z)) return -1;
				return InPlayerBlockPos.asInt(x, y, z);
			}
		} catch (Exception ignored) {}
		return -1;
	}
}
