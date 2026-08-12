package net.blockomorph.core.phys;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class EntityInsideCalculator {
	private final IntList currentBlocks = new IntArrayList(BlocksInPlayerStorage.ONE_AXIS);
	private final List<BlockInPlayer2> currentBlocksBounded = new ObjectArrayList<>(BlocksInPlayerStorage.ONE_AXIS);
	private final Entity owner;
	private final PlayerAccessor playerOwner;
	private final BlockPos.MutableBlockPos iterPos = new BlockPos.MutableBlockPos();
	private final int heavyMode;
	private final InsideBlockEffectApplier.StepBasedCollector applier;
	private LevelChunk cachedChunk;

	public EntityInsideCalculator(Entity entity, int heavyMode) {
		this.owner = entity;
		this.applier = new InsideBlockEffectApplier.StepBasedCollector();
		this.playerOwner = this.owner instanceof PlayerAccessor pl ? pl : null;
		this.heavyMode = heavyMode;
	}

	public boolean needSimplifyCalc() {
		return this.playerOwner != null && this.playerOwner.size() > this.heavyMode;
	}

	public boolean trySimplify() {
		if (this.needSimplifyCalc()) {
			this.playerOwner.getBlocksStorage().forEach(block -> this.currentBlocks.add(block.getOffsetAsInt()));
			this.currentBlocks.forEach(pos -> {
				BlockInPlayer2 block = this.playerOwner.getBlock(pos);
				if (block != null) {
					double realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
					double realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
					double realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
					this.iterPos.set(realX, realY, realZ);
					this.handleBlock(this.iterPos, this.getBlockFromWorld(this.iterPos), realX, realY, realZ);
				}
			});
			this.currentBlocks.clear();
			this.applier.applyAndClear(this.owner);
			this.cachedChunk = null;
			return true;
		}
		return false;
	}

	public void calculateForPlayersCollide() {
		if (!Config.get().entityInside.getValue()) return;
		PlayersStorage storage = PlayersStorage.ofLevel(this.owner.level());
		try (storage) {
			for (Player player : storage.findMorphedPlayers(this.owner, this.owner.getBoundingBox())) {
				var cursor = PlayerAccessor.of(player).getManager().getCursor3D();
				try (cursor) {
					for (BlockInPlayer2 block : cursor.forAllBlocks(this.owner.getBoundingBox())) {
						this.currentBlocksBounded.add(block);
					}
				}
			}
		}
		if (this.currentBlocksBounded.isEmpty()) return;
		this.currentBlocksBounded.forEach(block -> {
			double realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
			double realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
			double realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
			this.handleBlock(block.getPos(), block.getBlockState(), realX, realY, realZ);
		});
		this.currentBlocksBounded.clear();
		this.applier.applyAndClear(this.owner);
	}

	private BlockState getBlockFromWorld(BlockPos pos) {
		int chunkX = SectionPos.blockToSectionCoord(pos.getX());
		int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
		if (this.cachedChunk == null || this.cachedChunk.getPos().x != chunkX || this.cachedChunk.getPos().z != chunkZ) {
			this.cachedChunk = this.owner.level().getChunk(chunkX, chunkZ);
		}
		return this.cachedChunk.getBlockState(pos);
	}

	private void handleBlock(BlockPos realOrKey, BlockState state, double realX, double realY, double realZ) {
		this.handleBlockState(realOrKey, state, realX, realY, realZ);
		this.handleFluidState(realOrKey, state.getFluidState(), realX, realY, realZ);
	}

	private void handleBlockState(BlockPos realOrKey, BlockState state, double realX, double realY, double realZ) {
		if (this.containsBlockShape(state, realOrKey, realX, realY, realZ)) {
			this.applier.advanceStep(0);
			state.entityInside(this.owner.level(), realOrKey, this.owner, this.applier, true);
		}
	}

	private void handleFluidState(BlockPos realOrKey, FluidState state, double realX, double realY, double realZ) {
		if (state.isEmpty()) return;
		if (this.containsFluidShape(state, realOrKey, realX, realY, realZ)) {
			state.entityInside(this.owner.level(), realOrKey, this.owner, this.applier);
		}
	}

	private boolean containsBlockShape(BlockState state, BlockPos realOrKey, double realX, double realY, double realZ) {
		AABB hitbox = this.owner.getBoundingBox();
		VoxelShape intersectShape = state.getEntityInsideCollisionShape(this.owner.level(), realOrKey, this.owner);
		if (intersectShape == Shapes.block())
			return true;
		if (this.fullInclude(hitbox, realX, realY, realZ))
			return true;
		for (AABB aabb : intersectShape.toAabbs()) {
			if (hitbox.intersects(
					realX + aabb.minX, realY + aabb.minY, realZ + aabb.minZ,
					realX + aabb.maxX, realY + aabb.maxY, realZ + aabb.maxZ)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsFluidShape(FluidState state, BlockPos realOrKey, double realX, double realY, double realZ) {
		AABB hitbox = this.owner.getBoundingBox();
		if (this.fullInclude(hitbox, realX, realY, realZ))
			return true;
		float height = state.getHeight(this.owner.level(), realOrKey);
		return hitbox.intersects(realX, realY, realZ, realX + 1, realY + height, realZ + 1);
	}

	private boolean fullInclude(AABB hitbox, double realX, double realY, double realZ) {
		return hitbox.contains(realX, realY, realZ) && hitbox.contains(realX + 1, realY + 1, realZ + 1);
	}
}
