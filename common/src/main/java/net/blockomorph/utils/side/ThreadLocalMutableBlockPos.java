package net.blockomorph.utils.side;

import net.minecraft.core.BlockPos;

public class ThreadLocalMutableBlockPos extends MinecraftThreadLocal<BlockPos.MutableBlockPos> {

	public ThreadLocalMutableBlockPos() {
		super(false, BlockPos.MutableBlockPos::new);
	}

	public BlockPos.MutableBlockPos set(int x, int y, int z) {
		BlockPos.MutableBlockPos pos = super.get();
		if (pos != null) return pos.set(x, y, z);
		return null;
	}
}
