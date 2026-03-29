package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.accessors.FakeChunkStorage;
import net.blockomorph.utils.accessors.LevelAcc;
import net.blockomorph.utils.coords.DummyChunkStorage;
import net.blockomorph.utils.coords.DummyLevelChunk;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelInjectsMixin implements FakeChunkStorage {
	@Unique
	private DummyChunkStorage CHUNKS;

	@Inject(method = "isInWorldBoundsHorizontal", at = @At(value = "HEAD"), cancellable = true)
	private static void acceptIfPlayerPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX()))
			cir.setReturnValue(true);
	}

	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	public BlockState changeState(BlockState original, BlockPos pos) {
		AtomicReference<BlockState> value = new AtomicReference<>(original);
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			boolean isLiquid = original.getBlock() instanceof LiquidBlock;
			boolean lockLiquid = isLiquid && pl.isUnContextedBreaking();
			if (pl.isBreaking() && realPos.equals(InPlayerBlockPos.ZERO) && (original == Blocks.AIR.defaultBlockState() || lockLiquid)) {
				value.set(Blocks.VOID_AIR.defaultBlockState());
			} else if (lockLiquid) {
				value.set(Blocks.AIR.defaultBlockState());
			}
		}, null, LevelAcc.of(this));
		return value.get();
	}

	@ModifyVariable(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("STORE"))
	private ChunkAccess override(ChunkAccess chunkAccess, int x, int z) {
		if (InPlayerBlockPos.isMorphedPlayerX(SectionPos.sectionToBlockCoord(x))) {
			DummyChunkStorage storage;
			if (LevelAcc.of(this).getChunkSource() instanceof FakeChunkStorage st) {
				storage = st.getStorage();
			} else {
				if (CHUNKS == null) {
					CHUNKS = new DummyChunkStorage();
				}
				storage = CHUNKS;
			}
			return storage.getFakeChunk(x, z, LevelAcc.of(this));
		}
		return chunkAccess;
	}

	@Override
	public DummyChunkStorage getStorage() {
		return CHUNKS;
	}

	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void fixBlockGetting(BlockPos blockPos, CallbackInfoReturnable<BlockState> cir) {
		DummyLevelChunk.getBlockState(LevelAcc.of(this), blockPos, cir);
	}

	@ModifyVariable(method = {"destroyBlock", "removeBlock"}, at = @At("STORE"))
	private FluidState changeFluid(FluidState orig, BlockPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) return Fluids.EMPTY.defaultFluidState();
		return orig;
	}

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
	public void rejectBreak(BlockPos pos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (pl.isOnLoadingBlocks() && blockState == Blocks.AIR.defaultBlockState() && realPos.equals(InPlayerBlockPos.ZERO))
				cir.setReturnValue(false);
		}, () -> cir.setReturnValue(false), LevelAcc.of(this));

	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"))
	private Predicate<Entity> eraseOwner(Predicate<Entity> original, Entity ent, AABB aabb) {
		AtomicReference<Player> player = new AtomicReference<>();
		InPlayerBlockPos.check(BlockPos.containing(aabb.getCenter()), (pl, realPos) -> {
			player.set(pl.player());
		}, null, LevelAcc.of(this));
		return original.and((entity -> entity != player.get()));
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V", at = @At("HEAD"))
	private <T extends Entity> Predicate<Entity> eraseOwner(Predicate<Entity> original, EntityTypeTest<Entity, T> type, AABB aabb) {
		AtomicReference<Player> player = new AtomicReference<>();
		InPlayerBlockPos.check(BlockPos.containing(aabb.getCenter()), (pl, realPos) -> {
			player.set(pl.player());
		}, null, LevelAcc.of(this));
		return original.and((entity -> entity != player.get()));
	}
}
