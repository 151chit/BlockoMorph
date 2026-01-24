package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelLightEngine.class)
public class LightEngineManagerMixin {

	@ModifyVariable(method = "getRawBrightness", at = @At("HEAD"))
	public BlockPos getRealRaw(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}
}
