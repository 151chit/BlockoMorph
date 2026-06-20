package net.blockomorph.utils.playerSection;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphMath;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerPerBlockHandler {//todo: temp
	private final PlayerAccessor pl;
	private final Player player;

	protected PlayerPerBlockHandler(Player player) {
		this.pl = PlayerAccessor.of(player);
		this.player = player;
	}

	public void onMove(Vec3 newPos) {
		this.removeAll();
		this.pl.getBlocksData2().values().forEach(block ->
				this.onBlockAddInternal(block, newPos, this.pl.minPos(), this.pl.maxPos()));
	}

	public void internalBoxChanged(InPlayerBlockPos oldMin, InPlayerBlockPos oldMax, InPlayerBlockPos min, InPlayerBlockPos max) {
		this.pl.getBlocksData2().values().forEach(block -> {
			this.onBlockRemovedInternal(block, oldMin, oldMax);
			this.onBlockAddInternal(block, this.player.position(), min, max);
		});
	}

	public void onBlockRemoved(BlockInPlayer2 block) {
		this.onBlockRemovedInternal(block, this.pl.minPos(), this.pl.maxPos());
	}

	protected void removeAll() {
		this.pl.getBlocksData2().values().forEach(this::onBlockRemoved);
	}

	private void onBlockAddInternal(BlockInPlayer2 block, Vec3 playerPos, InPlayerBlockPos min, InPlayerBlockPos max) {
		if (this.player.level() instanceof PlayersProvider provider)
			provider.getStorage$blockomorph().getMutablePlayersPerBlock().computeIfAbsent(
					this.getBlockRealPos(block, playerPos, min, max), l -> new PlayersMultiSectionStorage.PlayersInBlockSet()).add(block);
	}

	private void onBlockRemovedInternal(BlockInPlayer2 block, InPlayerBlockPos min, InPlayerBlockPos max) {
		if (this.player.level() instanceof PlayersProvider provider) {
			long pos = this.getBlockRealPos(block, this.player.position(), min, max);
			var storage = provider.getStorage$blockomorph().getMutablePlayersPerBlock();
			var blocksSet = storage.get(pos);
			if (blocksSet != null) {
				blocksSet.remove(block);
				if (blocksSet.isEmpty()) storage.remove(pos);
			}
		}
	}

	private long getBlockRealPos(BlockInPlayer2 block, Vec3 playerPos, InPlayerBlockPos min, InPlayerBlockPos max) {
		InPlayerBlockPos offset = block.getOffset();
		Vec3 realPos = MorphMath.getRealBlockPos(playerPos, min, max, offset.x + 0.5, offset.y, offset.z + 0.5);
		return BlockPos.asLong(Mth.floor(realPos.x), Mth.floor(realPos.y), Mth.floor(realPos.z));
	}
}
