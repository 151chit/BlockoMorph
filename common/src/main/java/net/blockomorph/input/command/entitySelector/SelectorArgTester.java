package net.blockomorph.input.command.entitySelector;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public class SelectorArgTester {

	static boolean test(PlayerAccessor testing, SelectorArgType.BlockInPlayerTester blockPredicate) {
		int pos = InPlayerBlockPos.asInt(blockPredicate.x(), blockPredicate.y(), blockPredicate.z());
		BlockInPlayer2 blockInPlayer = testing.getBlock(pos);

		Optional<BlockStateParser.BlockResult> blockTest = blockPredicate.block().left();
		if (blockTest.isPresent()) {
			BlockStateParser.BlockResult block = blockTest.get();
			return validateWithBlock(blockInPlayer, block.blockState(), block.properties(), block.nbt());
		}
		BlockStateParser.TagResult tagResult = blockPredicate.block().right().orElseThrow();
		return validateWithTags(blockInPlayer, tagResult.tag(), tagResult.vagueProperties(), tagResult.nbt());
	}

	private static boolean validateWithBlock(BlockInPlayer2 block, BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
		if (block == null) {
			return blockState.is(Blocks.AIR);
		}
		BlockState stateInBlock = block.getBlockState();
		if (!blockState.is(stateInBlock.getBlock())) return false;

		for(Property<?> property : properties.keySet()) {
			if (stateInBlock.getValue(property) != blockState.getValue(property)) {
				return false;
			}
		}

		if (nbt == null) {
			return true;
		} else {
			BlockEntity entity = block.getBlockEntity();
			return entity != null && NbtUtils.compareNbt(nbt, entity.saveWithFullMetadata(block.getOwner().level().registryAccess()), true);
		}
	}

	private static boolean validateWithTags(BlockInPlayer2 block, HolderSet<Block> tags, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
		if (block == null) {
			return Blocks.AIR.defaultBlockState().is(tags);
		}

		BlockState stateInBlock = block.getBlockState();
		if (!stateInBlock.is(tags)) return false;

		for(Map.Entry<String, String> entry : vagueProperties.entrySet()) {
			Property<?> property = stateInBlock.getBlock().getStateDefinition().getProperty(entry.getKey());
			if (property == null) {
				return false;
			}

			Comparable<?> value = property.getValue(entry.getValue()).orElse(null);
			if (value == null) {
				return false;
			}

			if (stateInBlock.getValue(property) != value) {
				return false;
			}
		}

		if (nbt == null) {
			return true;
		} else {
			BlockEntity entity = block.getBlockEntity();
			return entity != null && NbtUtils.compareNbt(nbt, entity.saveWithFullMetadata(block.getOwner().level().registryAccess()), true);
		}
	}
}
