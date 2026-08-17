package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockInput.class)
public abstract class BlockInputAcc implements Accessors.BlockAccessor {
	@Shadow @Final private CompoundTag tag;
	@Override
	public CompoundTag getTag$bm() {
		return this.tag;
	}
}
