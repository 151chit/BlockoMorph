package net.blockomorph.core.phys.fluid;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class FluidWorker {
	private Object lastKey;
	private FluidTracker lastTracker;

	protected abstract Object keyForBlock(BlockInPlayer2 block);
	protected abstract FluidTracker trackerForBlock(BlockInPlayer2 block);

	protected FluidState blockToFluidState(BlockInPlayer2 block) {
		return block.getBlockState().getFluidState();
	}

	public void handleFluid(Entity self, AABB fluidCollisionBox, boolean ignoreCurrent) {
		PlayersStorage storage = PlayersStorage.ofLevel(self.level());
		try (storage) {
			for (Player player : storage.findMorphedPlayers(self, fluidCollisionBox)) {
				var cursor = PlayerAccessor.of(player).getManager().getCursor3D();
				try (cursor) {
					for (BlockInPlayer2 block : cursor.forAllBlocks(fluidCollisionBox)) {
						if (block.shouldDoFluidAction()) {
							FluidState fluidState = block.getBlockState().getFluidState();
							if (!fluidState.isEmpty()) {
								handleBlock(self, fluidCollisionBox, block, ignoreCurrent);
							}
						}
					}
				}
			}
		}
	}

	private void handleBlock(Entity self, AABB collideBox, BlockInPlayer2 block, boolean ignoreCurrent) {
		FluidState fluidState = block.getBlockState().getFluidState();
		double fluidBottom = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
		double fluidTop = fluidBottom + fluidState.getHeight(self.level(), block.getPos());
		if (!(fluidTop < collideBox.minY)) {
			this.cacheTracker(block);
			if (this.lastTracker != null) {
				this.lastTracker.doAdditional$bm();
				this.handleEyeInside(block, self, fluidBottom, fluidTop);
				this.lastTracker.setFluidHeight$bm(Math.max(fluidTop - self.getBoundingBox().minY, this.lastTracker.getFluidHeight$bm()));
				if (!ignoreCurrent) this.applyMovement(self, block);
			}
		}
	}

	private void cacheTracker(BlockInPlayer2 block) {
		Object idKey = this.keyForBlock(block);
		if (this.lastKey != idKey) {
			this.lastKey = idKey;
			this.lastTracker = this.trackerForBlock(block);
		}
	}

	private void handleEyeInside(BlockInPlayer2 block, Entity self, double fluidBottom, double fluidTop) {
		Vec3 entityPos = self.position();
		double eyeY = self.getEyeY();
		double realCornerX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
		double realCornerZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
		if (entityPos.z > realCornerZ && entityPos.z() < realCornerZ + 1 &&
				entityPos.x > realCornerX && entityPos.x < realCornerX + 1 &&
				eyeY >= fluidBottom && eyeY <= fluidTop) {
			this.lastTracker.eyeInside$bm();
		}
	}

	private void applyMovement(Entity self, BlockInPlayer2 block) {
		Vec3 flow = block.getBlockState().getFluidState().getFlow(self.level(), block.getPos());
		if (this.lastTracker.getFluidHeight$bm() < 0.4) {
			flow = flow.scale(this.lastTracker.getFluidHeight$bm());
		}
		this.lastTracker.accumulateCurrent$bm(flow);
	}
}
