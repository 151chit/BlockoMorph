package net.blockomorph.core.serialization;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.core.PlayerAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public record BlockPalette(List<BlockState> ids, int[] poses, int[] types) {

	public static BlockPalette singleBlock(int pos, BlockState state) {
		ArrayList<BlockState> singlePalette = new ArrayList<>();
		singlePalette.add(state);
		return new BlockPalette(singlePalette, new int[]{pos}, new int[]{0});
	}

	public static BlockPalette packAll(BlocksInPlayerStorage storage, Consumer<BlockInPlayer2> blockCallback) {
		int size = storage.size();

		Object2IntOpenHashMap<BlockState> stateToIdMap = new Object2IntOpenHashMap<>();
		stateToIdMap.defaultReturnValue(-1);
		ArrayList<BlockState> paletteList = new ArrayList<>();

		int i = 0;
		int[] poses = new int[size];
		int[] types = new int[size];
		for (BlockInPlayer2 block : storage) {
			packBlock(poses, types, block.getBlockState(), block.getOffset().asInt(), stateToIdMap, paletteList, i);
			blockCallback.accept(block);
			i++;
		}
		return new BlockPalette(paletteList, poses, types);
	}

	public static BlockPalette packWithFilter(BlocksInPlayerStorage storage, IntSet posFilter, IntFunction<BlockState> stateFallback, Consumer<BlockInPlayer2> blockCallback) {
		IntIterator it = posFilter.intIterator();
		Object2IntOpenHashMap<BlockState> stateToIdMap = new Object2IntOpenHashMap<>();
		stateToIdMap.defaultReturnValue(-1);
		ArrayList<BlockState> paletteList = new ArrayList<>();
		int i = 0;
		int[] poses = new int[posFilter.size()];
		int[] types = new int[posFilter.size()];
		while (it.hasNext()) {
			int pos = it.nextInt();
			BlockInPlayer2 block = storage.get(pos);
			if (block != null) {
				packBlock(poses, types, block.getBlockState(), block.getOffset().asInt(), stateToIdMap, paletteList, i);
				blockCallback.accept(block);
			} else packBlock(poses, types, stateFallback.apply(pos), pos, stateToIdMap, paletteList, i);
			i++;
		}
		return new BlockPalette(paletteList, poses, types);
	}

	public static BlockPalette packRaw(Int2ObjectMap<BlockState> states) {
		Object2IntOpenHashMap<BlockState> stateToIdMap = new Object2IntOpenHashMap<>();
		stateToIdMap.defaultReturnValue(-1);
		ArrayList<BlockState> paletteList = new ArrayList<>();
		int[] i = new int[]{0};
		int[] poses = new int[states.size()];
		int[] types = new int[states.size()];
		states.forEach((pos, state) ->
			packBlock(poses, types, state, pos, stateToIdMap, paletteList, i[0]++)
		);
		return new BlockPalette(paletteList, poses, types);
	}

	private static void packBlock(int[] poses, int[] types, BlockState state, int pos, Object2IntOpenHashMap<BlockState> stateToIdMap,
						   ArrayList<BlockState> paletteList, int i) {
		int paletteIndex = stateToIdMap.getInt(state);
		if (paletteIndex == -1) {
			paletteIndex = paletteList.size();
			paletteList.add(state);
			stateToIdMap.put(state, paletteIndex);
		}

		poses[i] = pos;
		types[i] = paletteIndex;
	}

	public CompoundTag toNbt() {
		CompoundTag blockStates = new CompoundTag();
		ListTag paletteNbt = new ListTag();
		//noinspection ForLoopReplaceableByForEach
		for (int n = 0; n < this.ids.size(); n++) {
			paletteNbt.add(NbtUtils.writeBlockState(this.ids.get(n)));
		}
		blockStates.put("palette", paletteNbt);
		blockStates.putIntArray("poses", this.poses);
		blockStates.putIntArray("types", this.types);
		return blockStates;
	}

	public ClientBoundMorphUpdatePacket toPacket(PlayerAccessor player) {
		return new ClientBoundMorphUpdatePacket(this, player);
	}
}
