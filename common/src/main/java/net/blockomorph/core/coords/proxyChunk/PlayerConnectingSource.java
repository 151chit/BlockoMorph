package net.blockomorph.core.coords.proxyChunk;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.core.BlockPos;

public interface PlayerConnectingSource {
	PlayerConnectingSource AUTOMATIC = new PlayerConnectingSource() {
		private final static ThreadLocalMutableBlockPos EXTERNAL_GET = new ThreadLocalMutableBlockPos();

		@Override
		public InPlayerManager managerByPos(int x, int z) {
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(x, z);
			if (pl != null)
				return pl.getManager();
			return null;
		}

		@Override
		public BlockPos.MutableBlockPos externalPosHolder() {
			return EXTERNAL_GET.get();
		}
	};

	InPlayerManager managerByPos(int x, int z);
	BlockPos.MutableBlockPos externalPosHolder();

	default InPlayerManager managerByPos(BlockPos pos) {
		return this.managerByPos(pos.getX(), pos.getZ());
	}
}