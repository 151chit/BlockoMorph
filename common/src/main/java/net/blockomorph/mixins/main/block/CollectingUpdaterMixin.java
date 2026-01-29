package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.InstantNeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
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

	@Inject(cancellable = true, method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V", at = @At("HEAD"))
	public void update(BlockPos pos1, Block block, Orientation orientation, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.neighborChanged(pos1, block, orientation);
		});
	}

	@Inject(cancellable = true, method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V", at = @At("HEAD"))
	public void update(BlockState blockState, BlockPos pos1, Block block, Orientation orientation, boolean bl, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.neighborChanged(blockState, pos1, block, orientation, bl);
		});
	}

	@Inject(method = "updateNeighborsAtExceptFromFacing", at = @At("HEAD"), cancellable = true)
	public void update(BlockPos pos1, Block block, Direction direction, Orientation orientation, CallbackInfo ci) {
		this.check(ci, pos1, () -> {
			UPDATER.updateNeighborsAtExceptFromFacing(pos1, block, direction, orientation);
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
