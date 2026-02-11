package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.InstantNeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CollectingNeighborUpdater.class)
public class CollectingUpdaterMixin {
	@Shadow @Final private Level level;
	@Unique private InstantNeighborUpdater UPDATER;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(Level lv, int max, CallbackInfo ci) {
		this.UPDATER = new InstantNeighborUpdater(lv);
	}

	@Inject(method = "shapeUpdate", at = @At("HEAD"), cancellable = true)
	public void update(Direction direction, BlockState state, BlockPos pos1, BlockPos pos2, int flag, int dist, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.shapeUpdate(direction, state, pos1, pos2, flag, dist);
		});
	}

	@Inject(cancellable = true, method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"))
	public void update(BlockPos pos1, Block block, BlockPos pos2, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.neighborChanged(pos1, block, pos2);
		});
	}

	@Inject(cancellable = true, method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"))
	public void update(BlockState state, BlockPos pos1, Block block, BlockPos pos2, boolean piston, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.neighborChanged(state, pos1, block, pos2, piston);
		});
	}

	@Inject(method = "updateNeighborsAtExceptFromFacing", at = @At("HEAD"), cancellable = true)
	public void update(BlockPos pos1, Block block, Direction direction, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.updateNeighborsAtExceptFromFacing(pos1, block, direction);
		});
	}

	private void check(CallbackInfo ci, BlockPos pos, Runnable action) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (pl.isBreaking()) {
				ci.cancel();
				action.run();
			}
		}, ci::cancel, this.level);
	}
}
