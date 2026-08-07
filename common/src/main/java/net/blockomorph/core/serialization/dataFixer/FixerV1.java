package net.blockomorph.core.serialization.dataFixer;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.serialization.BlockPalette;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

class FixerV1 extends DataFixerHandler.DataFixer {

	@Override
	String oldTagRootName() {
		return "BlockMorph";
	}

	@Override
	CompoundTag tryFixRootTag(UUID playerId, RegistryAccess registry, CompoundTag v1) {
		CompoundTag main = new CompoundTag();

		BlockState state = NbtUtils.readBlockState(registry.lookupOrThrow(Registries.BLOCK), v1.getCompoundOrEmpty("BlockState"));
		if (state.is(Blocks.AIR)) {
			MorphUtils.LOGGER.error("Empty block in V1 data for player: {}", playerId);
			return null;
		}
		main.put(PlayerWorldSerializer.STATE_TAG, BlockPalette.singleBlock(InPlayerBlockPos.ZERO_INT, state).toNbt());

		CompoundTag tags = v1.getCompound("Tags").orElse(null);
		if (tags != null && !tags.isEmpty()) {
			CompoundTag rootBeTag = new CompoundTag();
			CompoundTag beTag = new CompoundTag();
			beTag.put("data", tags);
			rootBeTag.put(InPlayerBlockPos.ZERO_INT + "", beTag);
			main.put(PlayerWorldSerializer.BE_TAG, rootBeTag);
		}
		return main;
	}
}
