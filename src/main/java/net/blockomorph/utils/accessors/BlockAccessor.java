package net.blockomorph.utils.accessors;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.nbt.CompoundTag;

public interface BlockAccessor {
   CompoundTag getTag();

   static BlockAccessor of(BlockInput input) {
      return (BlockAccessor) input;
   }
}
