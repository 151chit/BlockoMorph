package net.blockomorph.core.coords.proxyChunk;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.side.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class ProxyPlayerChunk extends LevelChunk implements PlayerConnectingSource {
	private final BlockPos.MutableBlockPos externalGet = new BlockPos.MutableBlockPos();
	private final Level level;
	private InPlayerManager cachedManager;

	protected ProxyPlayerChunk(Level level, ChunkPos boundedBlockPos) {
		super(level, boundedBlockPos);
		this.level = level;
	}

	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean flags) {
		var oldBlock = ProxyPlayerChunkHandler.setBlockState(this, pos, state, flags);
		if (oldBlock == FastInject.CONTINUE_EXECUTION)
			return Blocks.AIR.defaultBlockState();
		return (BlockState) oldBlock;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		var block = ProxyPlayerChunkHandler.getBlockState(this, this.level, pos);
		if (block == FastInject.CONTINUE_EXECUTION)
			return Blocks.AIR.defaultBlockState();
		return (BlockState) block;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos, EntityCreationType p_62610_) {
		var be = ProxyPlayerChunkHandler.getBlockEntity(this, this.level, pos);
		if (be == FastInject.CONTINUE_EXECUTION) return null;
		return (BlockEntity) be;
	}

	@Override
	public FluidState getFluidState(int x, int y, int z) {
		var fluid = ProxyPlayerChunkHandler.getFluidState(this, this.level, x, y, z);
		if (fluid == FastInject.CONTINUE_EXECUTION)
			return Fluids.EMPTY.defaultFluidState();
		return (FluidState) fluid;
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		ProxyPlayerChunkHandler.removeBlockEntity(this, pos);
	}

	@Override
	public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
		this.changeBlockEntity(blockEntity, true);
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		this.changeBlockEntity(blockEntity, false);
	}

	private void changeBlockEntity(BlockEntity blockEntity, boolean changeTicker) {
		ProxyPlayerChunkHandler.changeBlockEntity(this, blockEntity, changeTicker);
	}

	@Override
	public int getHeight(Heightmap.Types type, int x, int z) {
		ChunkPos pos = this.getPos();
		InPlayerManager mn = this.managerByPos(pos.getBlockX(x), pos.getBlockZ(z));
		if (mn != null) {
			for (int i = InPlayerBlockPos.Y_CHUNK_END; i >= InPlayerBlockPos.Y_CHUNK_START; i--) {
				int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos.getBlockX(x), i, pos.getBlockZ(z));
				if (posIn != -1 && type.isOpaque().test(mn.getBlockState(posIn))) {
					return i;
				}
			}
		}
		return InPlayerBlockPos.Y_CHUNK_START;
	}

	@Override
	public FullChunkStatus getFullStatus() {
		return FullChunkStatus.BLOCK_TICKING;
	}

	@Override
	public InPlayerManager managerByPos(int x, int z) {
		if (Side.get() == Side.UNKNOWN) return null;
		long section = MorphedPlayerSection.fromMorphedBlockPos(x, z);
		if (this.cachedManager == null || this.cachedManager.getOwner() == null ||
				this.cachedManager.getOwner().player().isRemoved() || this.cachedManager.getSectionId() != section) {
			this.cachedManager = PlayerConnectingSource.AUTOMATIC.managerByPos(x, z);
		}
		return this.cachedManager;
	}

	@Override
	public BlockPos.MutableBlockPos externalPosHolder() {
		return this.externalGet;
	}
}
