package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public class WorldBorderMixin {

	@Inject(method = "isWithinBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private void check(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		this.checkIfPlayerPos(pos.getX(), cir);
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
	private void check(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphPlayerChunk(pos))
			cir.setReturnValue(true);
	}

	@Inject(method = "isWithinBounds(DD)Z", at = @At("HEAD"), cancellable = true)
	private void check(double x, double z, CallbackInfoReturnable<Boolean> cir) {
		this.checkIfPlayerPos(x, cir);
	}

	@Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void check(double x, double z, double offset, CallbackInfoReturnable<Boolean> cir) {
		this.checkIfPlayerPos(x, cir);
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
	private void check(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
		this.checkIfPlayerPos(aabb.getCenter().x, cir);
	}

	private void checkIfPlayerPos(double x, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(x))
			cir.setReturnValue(true);
	}
}
