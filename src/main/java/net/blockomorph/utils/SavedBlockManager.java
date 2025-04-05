package net.blockomorph.utils;

import java.io.File;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.util.Map;
import java.nio.file.Path;

public class SavedBlockManager {
   private final Path gameDir;
   private boolean init;
   private final HashMap<String, SavedBlock> blocks = new HashMap<>();
   
   public SavedBlockManager(Path gameDir) {
   	  this.gameDir = gameDir.resolve("saved_blocks.bmdat");
   }

   public void load() {
   	  if (!init)
   	  try {
   	    CompoundTag tag = NbtIo.read(this.gameDir);
   	    if (tag != null) {
   	      for (String key : tag.getAllKeys()) {
   	  	    this.blocks.put(key, SavedBlock.fromTag(tag.getCompound(key), key));
   	      }
   	    }
   	    init = true;
   	  } catch (Exception e) {
   		e.printStackTrace();
   	  }
   }

   public HashMap<String, SavedBlock> get() {
   	  return this.blocks;
   }

   public void delete(String s) {
   	  if (this.blocks.containsKey(s)) {
   	    this.blocks.remove(s);
   	    this.write();
   	  }
   }

   public void add(SavedBlock b) {
   	  String s = b.getName();
   	  if (!this.blocks.containsKey(s)) {
   	  	this.blocks.put(s, b);
   	  	this.write();
   	  }
   }

   public void clear() {
   	  this.blocks.clear();
   	  this.write();
   }

   public void write() {
   	  try {
   	    CompoundTag tag = new CompoundTag();
   	    for (Map.Entry<String, SavedBlock> entry : this.blocks.entrySet()) {
   	  	  tag.put(entry.getKey(), entry.getValue().toNbt());
   	    }
   	    NbtIo.write(tag, this.gameDir);
   	  } catch (Exception e) {
   		e.printStackTrace();
   	  }
   }
}
