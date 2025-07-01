package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightEngine.class)
public class LightEngineMixin {

    @ModifyVariable(method = "getLightValue", at = @At("HEAD"))
    public BlockPos getReal(BlockPos orig) {
        return InPlayerBlockPos.checkOnReal(orig);
    }
}
