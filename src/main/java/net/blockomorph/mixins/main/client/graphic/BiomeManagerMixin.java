package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeManager.class)
public class BiomeManagerMixin {
    
    @ModifyVariable(method = "getBiome", at = @At("HEAD"))
    public BlockPos real(BlockPos value) {
        return InPlayerBlockPos.checkOnReal(value);
    }

    @ModifyVariable(method = "getNoiseBiomeAtPosition(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", at = @At("HEAD"))
    public BlockPos realNoise(BlockPos value) {
        return InPlayerBlockPos.checkOnReal(value);
    }

    @Unique
    private Vec3 tempPos;

    @Inject(method = "getNoiseBiomeAtPosition(DDD)Lnet/minecraft/core/Holder;", at = @At(value = "HEAD"))
    public void redirect(double x, double y, double z, CallbackInfoReturnable<Holder<Biome>> cir) {
        this.tempPos = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
    }

    @ModifyVariable(method = "getNoiseBiomeAtPosition(DDD)Lnet/minecraft/core/Holder;", at = @At(value = "HEAD"), ordinal = 0)
    public double getX(double value) {
        return tempPos.x;
    }

    @ModifyVariable(method = "getNoiseBiomeAtPosition(DDD)Lnet/minecraft/core/Holder;", at = @At(value = "HEAD"), ordinal = 1)
    public double getY(double value) {
        return tempPos.y;
    }

    @ModifyVariable(method = "getNoiseBiomeAtPosition(DDD)Lnet/minecraft/core/Holder;", at = @At(value = "HEAD"), ordinal = 2)
    public double getZ(double value) {
        return tempPos.z;
    }
}
