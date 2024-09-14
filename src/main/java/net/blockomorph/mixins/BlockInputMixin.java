package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

import net.blockomorph.utils.BlockAccessor;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.nbt.CompoundTag;

@Mixin(BlockInput.class)
public abstract class BlockInputMixin implements BlockAccessor {
   @Shadow @Final private CompoundTag tag;

   public CompoundTag getTag() {
   	return this.tag;
   }
}
