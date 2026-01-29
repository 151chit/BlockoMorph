package net.blockomorph.utils.coords;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class DummyLevelChunk extends EmptyLevelChunk {
	private final Level level;

	public DummyLevelChunk(Level lv, ChunkPos boundedSection) {
		super(lv, boundedSection, lv.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
		this.level = lv;
	}

	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean update) {
		CallbackInfoReturnable<BlockState> cir = new CallbackInfoReturnable<>("", true, Blocks.AIR.defaultBlockState());
		setBlockState(this.level, pos, state, update, cir);
		return cir.getReturnValue();
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		CallbackInfoReturnable<BlockState> cir = new CallbackInfoReturnable<>("", true, Blocks.AIR.defaultBlockState());
		getBlockState(this.level, pos, cir);
		return cir.getReturnValue();
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos, EntityCreationType p_62610_) {
		CallbackInfoReturnable<BlockEntity> cir = new CallbackInfoReturnable<>("", true, null);
		getBlockEntity(this.level, pos, cir);
		return cir.getReturnValue();
	}

	@Override
	public FluidState getFluidState(int x, int y, int z) {
		CallbackInfoReturnable<FluidState> cir = new CallbackInfoReturnable<>("", true, Fluids.EMPTY.defaultFluidState());
		getFluidState(this.level, new BlockPos(x, y, z), cir);
		return cir.getReturnValue();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		removeBlockEntity(this.level, pos, new CallbackInfo("", true));
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
		changeBlockEntity(this.level, blockEntity, changeTicker, new CallbackInfo("", true));
	}

	@Override
	public FullChunkStatus getFullStatus() {
		return FullChunkStatus.BLOCK_TICKING;
	}

	public static void setBlockState(Level level, BlockPos pos, BlockState state, boolean update, CallbackInfoReturnable<BlockState> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			cir.setReturnValue(pl.getBlockState(realPos));
			if (!pl.setBlockState(realPos, state, update))
				cir.setReturnValue(null);
		}, null, level);
	}

	public static void removeBlockEntity(Level level, BlockPos pos, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			ci.cancel();
			BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
			if (block != null)
				block.clearBlockEntity();
		}, ci::cancel, level);
	}

	public static void changeBlockEntity(Level level, BlockEntity blockEntity, boolean changeTicker, CallbackInfo ci) {
		InPlayerBlockPos.check(blockEntity.getBlockPos(), (pl, realPos) -> {
			ci.cancel();
			BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
			if (block != null)
				block.forceChangeBlockEntity(blockEntity, changeTicker);
		}, ci::cancel, level);
	}

	public static void getBlockState(Level level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			BlockState state = pl.getBlockState(realPos);
			if (state == null) {
				cir.setReturnValue(Blocks.AIR.defaultBlockState());
			} else if (level instanceof ClientLevelAccessor acc && acc.isExternalMorphedBlockGetterLocked()) {
				cir.setReturnValue(state);
			} else if (state.isAir() && !realPos.equals(InPlayerBlockPos.ZERO)) {
				Vec3 vec = MorphUtils.getCetneredRealBlockPos(pl, realPos);
				cir.setReturnValue(level.getBlockState(BlockPos.containing(vec)));
			} else {
				cir.setReturnValue(state);
			}
		}, null, level);
	}

	public static void getBlockEntity(Level level, BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			cir.setReturnValue(pl.getBlockEntity(realPos));
		}, null, level);
	}

	public static void getFluidState(Level level, BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			BlockState state = pl.getBlockState(realPos);
			if (state == null || (state.isAir() && !realPos.equals(InPlayerBlockPos.ZERO))) {
				Vec3 vec = MorphUtils.getCetneredRealBlockPos(pl, realPos);
				cir.setReturnValue(level.getFluidState(BlockPos.containing(vec)));
			} else {
				cir.setReturnValue(state.getFluidState());
			}
		}, null, level);
	}
}
