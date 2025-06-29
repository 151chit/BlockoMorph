package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = {"net.minecraft.client.renderer.block.ModelBlockRenderer$Cache"})
public class ModelRendererMixin {

	@ModifyVariable(method = "getLightColor", at = @At("HEAD"), require = 1)
	public BlockPos getReal(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@ModifyVariable(method = "getShadeBrightness", at = @At("HEAD"))
	public BlockPos getRealShade(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}
}
