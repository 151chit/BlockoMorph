package net.blockomorph.mixins.main.system.virtualization.normalize.level.color;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BiomeManager.class) @SuppressWarnings("Invalid_FI_return_type")
public abstract class BiomeManagerMixin {
	@Shadow
	public abstract Holder<Biome> getNoiseBiomeAtQuart(int quartX, int quartY, int quartZ);

	@FastInject(method = "getNoiseBiomeAtPosition(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", at = @At("HEAD"))
	private Object norm(BlockPos blockPos) {
		return this.checkNorm(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	@FastInject(method = "getNoiseBiomeAtPosition(DDD)Lnet/minecraft/core/Holder;", at = @At("HEAD"))
	private Object norm(double x, double y, double z) {
		return this.checkNorm(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}

	@Unique
	private Object checkNorm(int x, int y, int z) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			int normX = Mth.floor(MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.X));
			int normY = Mth.floor(MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Y));
			int normZ = Mth.floor(MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Z));
			return this.getNoiseBiomeAtQuart(QuartPos.fromBlock(normX), QuartPos.fromBlock(normY), QuartPos.fromBlock(normZ));
		}
		return FastInject.CONTINUE_EXECUTION;
	}

	@ModifyVariable(method = "getBiome", at = @At(value = "STORE"), ordinal = 0)
	public int getX(int absX, BlockPos pos) {
		return this.normCoord(absX, Direction.Axis.X, pos);
	}

	@ModifyVariable(method = "getBiome", at = @At(value = "STORE"), ordinal = 1)
	public int getY(int absY, BlockPos pos) {
		return this.normCoord(absY, Direction.Axis.Y, pos);
	}

	@ModifyVariable(method = "getBiome", at = @At(value = "STORE"), ordinal = 2)
	public int getZ(int absZ, BlockPos pos) {
		return this.normCoord(absZ, Direction.Axis.Z, pos);
	}

	@Unique
	private int normCoord(int orig, Direction.Axis axis, BlockPos input) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(input.getX())) {
			return Mth.floor(MorphNormalizer.normalizeOneOf(input, axis)) - 2;
		}
		return orig;
	}
}
