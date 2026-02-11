package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockTintCache.class)
public class BlockTintCacheMixin {

	@ModifyVariable(method = "getColor", at = @At("HEAD"))
	public BlockPos getReal(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}
}
