package net.blockomorph.utils.blockUpdate;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphMath;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RedstoneUpdateManager {
	protected final ObjectOpenHashSet<BlockPos> oldPoses = new ObjectOpenHashSet<>();
	protected final Player player;
	private Vec3 lastPosition;
	private InPlayerBlockPos lastMin, lastMax;
	private BlockPos zeroReal;
	private boolean enabled;

	public RedstoneUpdateManager(PlayerAccessor player) {
		this.player = player.player();
	}

	public void tick() {
		if (this.player.level().isClientSide()) return;
		Vec3 currPosition = this.player.position();
		PlayerAccessor pl = PlayerAccessor.of(this.player);
		if (this.shouldStop()) return;
		if (this.lastPosition == null || MorphMath.isBlockPosChanged(this.lastPosition, currPosition, this.lastMin, this.lastMax, pl.minPos(), pl.maxPos())) {
			this.enabled = true;
			if (this.zeroReal != null) {
				this.oldPoses.forEach(this::updateBlock);
			}

			this.lastPosition = currPosition;
			this.lastMin = pl.minPos();
			this.lastMax = pl.maxPos();
			this.zeroReal = InPlayerBlockPos.ZERO.boundedBlockPos(this.player);
			if (this.zeroReal == null)
				throw new UnsupportedOperationException("Cannot update redstone for player: " + this.player);

			this.oldPoses.clear();
			pl.getBlocksData2().keySet().forEach(block -> {
				Vec3 realPos = MorphMath.getRealBlockPos(this.lastPosition, this.lastMin, this.lastMax, block.x + 0.5, block.y, block.z + 0.5);
				this.collectSphere(BlockPos.containing(realPos));
			});
			this.oldPoses.forEach(this::updateBlock);
		}
	}

	protected boolean isEnabled() {
		return Config.get().dynamicRedstone.getValue();
	}

	private boolean shouldStop() {
		boolean enabled = this.isEnabled();
		if (!enabled) {
			if (this.enabled) {
				this.enabled = false;
				this.stop();
			}
			return true;
		}
		return false;
	}

	public void stop() {
		this.oldPoses.forEach(this::updateBlock);
		this.oldPoses.clear();
		this.lastPosition = null;
		this.lastMax = null;
		this.lastMin = null;
	}

	protected void updateBlock(BlockPos block) {
		if (this.canReceiveUpdate(block)) {
			Direction direction = this.selectUpdateDir(block);
			this.doUpdate(block, direction);
			this.checkPlayerBlock(block, direction);
		}
	}

	private void checkPlayerBlock(BlockPos realPos, Direction dir) {
		var blockMap = PlayersProvider.of(this.player.level()).getStorage$blockomorph().getPlayersOnPos(realPos.asLong());
		if (!blockMap.isEmpty()) {
			for (Object obj : blockMap.getIterateArray()) {
				if (obj instanceof BlockInPlayer2 block) {
					if (!EntitySelector.NO_SPECTATORS.test(block.getPlayer().player())) continue;
					this.doUpdate(block.getPos(), dir);
				}
			}
		}
	}

	private void doUpdate(BlockPos block, Direction dir) {
		BlockPos neighbour = block.relative(dir);
		this.player.level().neighborChanged(block, this.player.level().getBlockState(neighbour).getBlock(), neighbour);
	}

	protected void collectSphere(BlockPos center) {
		int cx = center.getX();
		int cy = center.getY();
		int cz = center.getZ();
		this.oldPoses.add(center);
		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				for (int z = -2; z <= 2; z++) {
					int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
					if (distance <= 2 && distance > 0) {
						this.oldPoses.add(new BlockPos(cx + x, cy + y, cz + z));
					}
				}
			}
		}
	}

	private Direction selectUpdateDir(BlockPos pos) {
		if (this.zeroReal.getX() == pos.getX() && this.zeroReal.getZ() == pos.getZ()) return Direction.DOWN;
		if (pos.getZ() != this.zeroReal.getZ()) {
			int offset = pos.getX() - this.zeroReal.getX();
			if (offset > 0) {
				return Direction.WEST;
			} else return Direction.EAST;
		}
		int offset = pos.getZ() - this.zeroReal.getZ();
		if (offset > 0) {
			return Direction.NORTH;
		}
		return Direction.SOUTH;
	}

	protected boolean canBlockTypeReceiveUpdate(BlockState blockState) {
		return blockState.isSignalSource() || BlockMethodDetector.NEIGHBOUR_UPDATE.hasMethodInClass(blockState.getBlock());
	}

	protected boolean canReceiveUpdate(BlockPos blockPos) {
		BlockState blockState = this.player.level().getBlockState(blockPos);
		if (this.canBlockTypeReceiveUpdate(blockState)) {
			return true;
		} else if (this.player.level() instanceof PlayersProvider pr) {
			var pls = pr.getStorage$blockomorph().getPlayersOnPos(blockPos.asLong());
			if (!pls.isEmpty()) {
				for (Object obj : pls.getIterateArray()) {
					if (obj instanceof BlockInPlayer2 block) {
						if (!EntitySelector.NO_SPECTATORS.test(block.getPlayer().player())) continue;
						if (this.canBlockTypeReceiveUpdate(block.getBlockState()))
							return true;
					}
				}
			}
		}
		return false;
	}
}
