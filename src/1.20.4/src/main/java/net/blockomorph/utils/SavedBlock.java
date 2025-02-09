package net.blockomorph.utils;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

public class SavedBlock {
   private BlockState blockstate;
   private CompoundTag nbt;
   private String key;
   
   public SavedBlock(BlockState state, CompoundTag nbt, String k) {
   	   this.blockstate = state;
   	   this.nbt = nbt;
   	   this.key = k;
   }

   public BlockState getState() {
   	   return this.blockstate;
   }

   public CompoundTag getTag() {
   	   return this.nbt;
   }

   public String getName() {
   	   return this.key;
   }

   public CompoundTag toNbt() {
   	   CompoundTag tag = new CompoundTag();
   	   tag.put("BlockState", NbtUtils.writeBlockState(this.blockstate));
   	   tag.put("Tag", this.nbt);
   	   return tag;
   }

   public static SavedBlock fromTag(CompoundTag tag, String k) {
   	   CompoundTag nbt = tag.getCompound("Tag");
   	   BlockState state = NbtUtils.readBlockState(Minecraft.getInstance().level.holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
   	   return new SavedBlock(state, nbt, k);
   }

   public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof SavedBlock e) {
            return (e.getState().equals(this.blockstate)) && (e.getTag().equals(this.nbt)) && (e.getName().equals(this.key));
        }
        return false;
   }
}
