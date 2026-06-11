package net.blockomorph.utils.hit;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ClipContextAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.playerSection.PlayersFinder;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PlayerHitResult {
	public static final Predicate<Entity> NOT_MORPHED_PLAYER = (entity -> !(entity instanceof PlayerAccessor pl) || !pl.isFullActive());
	public static final AABB CUBE = Shapes.block().bounds();

	@Nullable
	public static MorphedPlayerHitResult calculateMorphedPlayerHitResult(PlayersFinder finder, @Nullable Entity looker, Vec3 eyePosition, Vec3 reachVector, TriFunction<InPlayerBlockPos, BlockInPlayer2, Vec3, List<AABB>> shapeGetter) {

		AABB areaBetweenAndReachEnd = new AABB(eyePosition, reachVector);

		AtomicReference<MorphedPlayerHitResult> result = new AtomicReference<>();
		AtomicReference<Double> distanceToPartOfBlock = new AtomicReference<>(Double.MAX_VALUE);

		for (PlayerAccessor mob : finder.findMorphed(looker, areaBetweenAndReachEnd)) {
			mob.getBlocksData2InArea(areaBetweenAndReachEnd, (offset, block, offsetPosInWorld) -> {
				for (AABB partBlockShape : shapeGetter.apply(offset, block, offsetPosInWorld)) {
					Optional<Vec3> partHitResult = partBlockShape.clip(eyePosition, reachVector);
					if (partHitResult.isPresent()) {
						Vec3 res = partHitResult.get();
						double dist = eyePosition.distanceTo(res);
						if (dist < distanceToPartOfBlock.get() || isMainBlock(result.get(), res, offsetPosInWorld)) {
							distanceToPartOfBlock.set(dist);
							Direction dir = calculateHitDirection(res, partBlockShape);
							if (dir != null) {
								Vec3 inBlockOffset = new Vec3(res.x() - offsetPosInWorld.x, res.y() - offsetPosInWorld.y, res.z() - offsetPosInWorld.z);
								result.set(MorphedPlayerHitResult.of(
										mob,
										offset,
										dir,
										inBlockOffset.x == (int) inBlockOffset.x || inBlockOffset.y == (int) inBlockOffset.y || inBlockOffset.z == (int) inBlockOffset.z,
										inBlockOffset,
										res
								));
							}
						}
					}
				}
			});
		}

		return result.get();
	}

	private static List<AABB> getBoxesList(BlockGetter lv, Vec3 offsetPosInWorld, BlockInPlayer2 block, ClipContext.Block mode, ClipContext.Fluid fluidMode, @Nullable Entity looker) {

		VoxelShape blockShape = mode.get(block.getBlockState(), lv, block.getPos(), looker != null ? CollisionContext.of(looker) : CollisionContext.empty());
		blockShape = blockShape.move(offsetPosInWorld.x, offsetPosInWorld.y, offsetPosInWorld.z);
		List<AABB> boxes = new ArrayList<>(blockShape.toAabbs());

		FluidState fluidState = block.getBlockState().getFluidState();
		if (block.shouldDoFluidAction() && fluidMode.canPick(fluidState)) {
			VoxelShape fluidShape = fluidState.getShape(lv, block.getPos());
			fluidShape = fluidShape.move(offsetPosInWorld.x, offsetPosInWorld.y, offsetPosInWorld.z);
			boxes.addAll(fluidShape.toAabbs());
		}
		return boxes;
	}

	//If voxelshapes conflict with each other
	private static boolean isMainBlock(MorphedPlayerHitResult oldHit, Vec3 newVec, Vec3 newBlockOffsetInWorld) {
		return oldHit != null && oldHit.getRealLocation().equals(newVec) && containsAABB(CUBE.move(newBlockOffsetInWorld), newVec);
	}

	private static Direction calculateHitDirection(Vec3 hitVec, AABB boundingBox) {
		double xDist = Math.min(Math.abs(hitVec.x - boundingBox.minX), Math.abs(hitVec.x - boundingBox.maxX));
		double yDist = Math.min(Math.abs(hitVec.y - boundingBox.minY), Math.abs(hitVec.y - boundingBox.maxY));
		double zDist = Math.min(Math.abs(hitVec.z - boundingBox.minZ), Math.abs(hitVec.z - boundingBox.maxZ));

		if (xDist < yDist && xDist < zDist) {
			return hitVec.x < boundingBox.getCenter().x ? Direction.WEST : Direction.EAST;
		} else if (yDist < xDist && yDist < zDist) {
			return hitVec.y < boundingBox.getCenter().y ? Direction.DOWN : Direction.UP;
		} else {
			return hitVec.z < boundingBox.getCenter().z ? Direction.NORTH : Direction.SOUTH;
		}
	}


	private static boolean containsAABB(AABB ab, Vec3 tr) {
		double tolerance = 1.0E-7;
		double d = tr.x;
		double e = tr.y;
		double f = tr.z;
		return (d >= ab.minX - tolerance && d <= ab.maxX + tolerance) &&
				(e >= ab.minY - tolerance && e <= ab.maxY + tolerance) &&
				(f >= ab.minZ - tolerance && f <= ab.maxZ + tolerance);
	}

	public static void checkHitResult(Level level, Vec3 oldHitPos, ClipBlockStateContext ctx, Consumer<MorphedPlayerHitResult> ifGood) {
		Vec3 start = ctx.getFrom();
		MorphedPlayerHitResult hit = PlayerHitResult.calculateMorphedPlayerHitResult(PlayersMultiSectionStorage.fromLevel(level), null, start, ctx.getTo(), (offset, block, offsetPosInWorld) -> {
			if (!ctx.isTargetBlock().test(block.getBlockState())) return List.of();
			return List.of(CUBE.move(offsetPosInWorld));
		});
		if (hit != null && start.distanceTo(hit.getRealLocation()) < start.distanceTo(oldHitPos)) {
			ifGood.accept(hit);
		}
	}

	public static void checkHitResult(Vec3 oldHitPos, ClipContext ctx, Consumer<MorphedPlayerHitResult> ifGood) {
		ClipContextAccessor accessor = ClipContextAccessor.of(ctx);
		CollisionContext collisionContext = accessor.getContext();
		if (collisionContext instanceof EntityCollisionContext context) {
			Entity looker = context.getEntity();
			if (looker != null) {
				Vec3 start = ctx.getFrom();
				Level level = looker.level();
				MorphedPlayerHitResult hit = PlayerHitResult.calculateMorphedPlayerHitResult(PlayersMultiSectionStorage.fromLevel(looker.level()), looker, start, ctx.getTo(),  (offset, block, offsetPosInWorld) -> {
					return getBoxesList(level, offsetPosInWorld, block, accessor.getMode(), accessor.getFluidMode(), looker);
				});
				if (hit != null && start.distanceTo(hit.getRealLocation()) < start.distanceTo(oldHitPos)) {
					ifGood.accept(hit);
				}
			}
		}
	}
}
