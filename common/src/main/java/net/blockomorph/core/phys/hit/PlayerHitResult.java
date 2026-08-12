package net.blockomorph.core.phys.hit;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.Accessors;
import net.blockomorph.utils.side.MinecraftThreadLocal;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.BiFunction;

public class PlayerHitResult {
	private static final MinecraftThreadLocal<PartHit> CURRENT_PART = new MinecraftThreadLocal<>(false, PartHit::new);

	public static MorphedPlayerHitResult clipBlockInLine(Object blockGetter, ClipBlockStateContext ctx) {
		PlayersStorage storage = PlayersStorage.ofLevel(blockGetter);
		try (storage) {
			Vec3 from = ctx.getFrom();
			Vec3 to = ctx.getTo();
			MorphedPlayerHitResult old = null;
			for (Player player : storage.findMorphedPlayers(null, from, to)) {
				old = traverseBlocksInPlayer(from, to, player, ctx, (ctxInter, block, oldResult) -> {
					if (ctxInter.isTargetBlock().test(block.getBlockState())) {
						Vec3 fromCtx = ctxInter.getFrom();
						Vec3 toCtx = ctxInter.getTo();
						return oldOrNew(new MorphedPlayerHitResult(block, ctxInter.getTo(),
							Direction.getApproximateNearest(fromCtx.x - toCtx.x, fromCtx.y - toCtx.y, fromCtx.z - toCtx.z),
								false), oldResult, ctxInter.getFrom());
					}
					return oldResult;
				}, old);
			}
			return old;
		}
	}

	public static MorphedPlayerHitResult clip(Object blockGetter, ClipContext ctx) {
		PlayersStorage storage = PlayersStorage.ofLevel(blockGetter);
		Entity looker = null;
		if (Accessors.ClipContextAccessor.of(ctx).getCollisionCtx$bm() instanceof EntityCollisionContext entityCtx) {
			looker = entityCtx.getEntity();
			if (!MorphUtils.canUseMorphLogicForProjectile(looker)) return null;
		}
		try (storage) {
			Vec3 from = ctx.getFrom();
			Vec3 to = ctx.getTo();
			MorphedPlayerHitResult old = null;
			for (Player player : storage.findMorphedPlayers(looker, from, to)) {
				old = traverseBlocksInPlayer(from, to, player, ctx, (ctxInter, block, oldResult) -> {
					var blockResult = checkCollideWithBlock(block, ctxInter, (ctxIn, blockData) ->
						ctxIn.getBlockShape(blockData.getBlockState(), blockData.getOwner().level(), blockData.getPos())
					);
					var fluidResult = checkCollideWithBlock(block, ctxInter, (ctxIn, blockData) ->
							blockData.shouldDoFluidAction() ?
						ctxIn.getFluidShape(blockData.getBlockState().getFluidState(), blockData.getOwner().level(), blockData.getPos()) : Shapes.empty()
					);
					if (blockResult != null || fluidResult != null) {
						double blockDistance = blockResult != null ? blockResult.distanceToRealSqr(ctxInter.getFrom()) : Double.MAX_VALUE;
						double fluidDistance = fluidResult != null ? fluidResult.distanceToRealSqr(ctxInter.getFrom()) : Double.MAX_VALUE;
						var mainResult = blockDistance <= fluidDistance ? blockResult : fluidResult;
						return oldOrNew(mainResult, oldResult, ctxInter.getFrom());
					}
					return oldResult;
				}, old);
			}
			return old;
		}
	}

	private static MorphedPlayerHitResult oldOrNew(MorphedPlayerHitResult mainResult, MorphedPlayerHitResult oldResult, Vec3 from) {
		if (oldResult == null || mainResult.distanceToRealSqr(from) <= oldResult.distanceToRealSqr(from)) {
			return mainResult;
		}
		return oldResult;
	}

	private static MorphedPlayerHitResult checkCollideWithBlock(BlockInPlayer2 block, ClipContext ctx, BiFunction<ClipContext, BlockInPlayer2, VoxelShape> getter) {
		PartHit hit = CURRENT_PART.get();
		hit.distanceToSqr = Double.MAX_VALUE;
		hit.oldPoint = null;
		hit.realX = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
		hit.realY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
		hit.realZ = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
		VoxelShape shape = getter.apply(ctx, block);
		if (shape == Shapes.empty()) return null;
		if (shape == Shapes.block()) return checkCollideWithCube(block, hit, ctx);
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			double minXn = hit.realX + minX;
			double minYn = hit.realY + minY;
			double minZn = hit.realZ + minZ;
			double maxXn = hit.realX + maxX;
			double maxYn = hit.realY + maxY;
			double maxZn = hit.realZ + maxZ;
			Vec3 from = ctx.getFrom();
			Optional<Vec3> pointCandidate = AABB.clip(minXn, minYn, minZn, maxXn, maxYn, maxZn, from, ctx.getTo());
			if (pointCandidate.isPresent()) {
				Vec3 point = pointCandidate.get();
				if (hit.oldPoint == null || point.distanceToSqr(from) < hit.distanceToSqr) {
					hit.oldPoint = point;
					hit.distanceToSqr = point.distanceToSqr(from);
					hit.direction = calculateHitSide(point, minXn, minYn, minZn, maxXn, maxYn, maxZn);
					hit.isInside = isInsideCube(minXn, minYn, minZn, maxXn, maxYn, maxZn, from);
				}
			}
		});
		if (hit.oldPoint != null) return new MorphedPlayerHitResult(block, hit.oldPoint, hit.direction, hit.isInside);
		return null;
	}

	private static MorphedPlayerHitResult checkCollideWithCube(BlockInPlayer2 block, PartHit hit, ClipContext ctx) {
		double minXn = hit.realX;
		double minYn = hit.realY;
		double minZn = hit.realZ;
		double maxXn = hit.realX + 1;
		double maxYn = hit.realY + 1;
		double maxZn = hit.realZ + 1;
		Vec3 from = ctx.getFrom();
		Optional<Vec3> pointCandidate = AABB.clip(minXn, minYn, minZn, maxXn, maxYn, maxZn, from, ctx.getTo());
		if (pointCandidate.isPresent()) {
			Vec3 point = pointCandidate.get();
			return new MorphedPlayerHitResult(block, point,
					calculateHitSide(point, minXn, minYn, minZn, maxXn, maxYn, maxZn),
					isInsideCube(minXn, minYn, minZn, maxXn, maxYn, maxZn, from));
		}
		return null;
	}

	private static class PartHit {
		double distanceToSqr; boolean isInside; Direction direction; Vec3 oldPoint; double realX; double realY; double realZ;
	}

	private static boolean isInsideCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3 point) {
		return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY && point.z >= minZ && point.z <= maxZ;
	}

	private static Direction calculateHitSide(Vec3 absoluteHit, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double xDist = Math.min(Math.abs(absoluteHit.x - minX), Math.abs(absoluteHit.x - maxX));
		double yDist = Math.min(Math.abs(absoluteHit.y - minY), Math.abs(absoluteHit.y - maxY));
		double zDist = Math.min(Math.abs(absoluteHit.z - minZ), Math.abs(absoluteHit.z - maxZ));

		if (xDist < yDist && xDist < zDist) {
			return absoluteHit.x < Mth.lerp(0.5, minX, maxX) ? Direction.WEST : Direction.EAST;
		} else if (yDist < xDist && yDist < zDist) {
			return absoluteHit.y < Mth.lerp(0.5, minY, maxY) ? Direction.DOWN : Direction.UP;
		} else {
			return absoluteHit.z < Mth.lerp(0.5, minZ, maxZ) ? Direction.NORTH : Direction.SOUTH;
		}
	}

	private static <CTX> MorphedPlayerHitResult traverseBlocksInPlayer(Vec3 fromReal, Vec3 toReal, Player player, CTX context,
	                                                                   BlockCollideTester<CTX> tester, MorphedPlayerHitResult oldHit) {
		if (fromReal.equals(toReal)) {
			return null;
		} else {
			PlayerAccessor pl = PlayerAccessor.of(player);
			double realX = MorphMath.getRealBlockPosCenter(Direction.Axis.X, pl);
			double realY = MorphMath.getRealBlockPosCenter(Direction.Axis.Y, pl);
			double realZ = MorphMath.getRealBlockPosCenter(Direction.Axis.Z, pl);
			double toX = Mth.lerp(-1.0E-7, toReal.x - realX, fromReal.x - realX);
			double toY = Mth.lerp(-1.0E-7, toReal.y - realY, fromReal.y - realY);
			double toZ = Mth.lerp(-1.0E-7, toReal.z - realZ, fromReal.z - realZ);
			double fromX = Mth.lerp(-1.0E-7, fromReal.x - realX, toReal.x - realX);
			double fromY = Mth.lerp(-1.0E-7, fromReal.y - realY, toReal.y - realY);
			double fromZ = Mth.lerp(-1.0E-7, fromReal.z - realZ, toReal.z - realZ);
			int currentBlockX = Mth.floor(fromX);
			int currentBlockY = Mth.floor(fromY);
			int currentBlockZ = Mth.floor(fromZ);
			BlockInPlayer2 block = pl.getBlock(InPlayerBlockPos.asInt(currentBlockX, currentBlockY, currentBlockZ));
			var hitResult = block != null ? tester.ifSuccess(context, block, oldHit) : null;
			if (hitResult != null) {
				return hitResult;
			} else {
				double dx = toX - fromX;
				double dy = toY - fromY;
				double dz = toZ - fromZ;
				int signX = Mth.sign(dx);
				int signY = Mth.sign(dy);
				int signZ = Mth.sign(dz);
				double tDeltaX = signX == 0 ? Double.MAX_VALUE : (double)signX / dx;
				double tDeltaY = signY == 0 ? Double.MAX_VALUE : (double)signY / dy;
				double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : (double)signZ / dz;
				double tX = tDeltaX * (signX > 0 ? (double)1.0F - Mth.frac(fromX) : Mth.frac(fromX));
				double tY = tDeltaY * (signY > 0 ? (double)1.0F - Mth.frac(fromY) : Mth.frac(fromY));
				double tZ = tDeltaZ * (signZ > 0 ? (double)1.0F - Mth.frac(fromZ) : Mth.frac(fromZ));
				while(tX <= (double)1.0F || tY <= (double)1.0F || tZ <= (double)1.0F) {
					if (tX < tY) {
						if (tX < tZ) {
							currentBlockX += signX;
							tX += tDeltaX;
						} else {
							currentBlockZ += signZ;
							tZ += tDeltaZ;
						}
					} else if (tY < tZ) {
						currentBlockY += signY;
						tY += tDeltaY;
					} else {
						currentBlockZ += signZ;
						tZ += tDeltaZ;
					}

					BlockInPlayer2 blockInPl = pl.getBlock(InPlayerBlockPos.asInt(currentBlockX, currentBlockY, currentBlockZ));
					var hitResultEnd = blockInPl != null ? tester.ifSuccess(context, blockInPl, oldHit) : null;
					if (hitResultEnd != null) {
						return hitResultEnd;
					}
				}
				return null;
			}
		}
	}

	@FunctionalInterface
	private interface BlockCollideTester<CTX> {
		MorphedPlayerHitResult ifSuccess(CTX ctx, BlockInPlayer2 block, MorphedPlayerHitResult oldResult);
	}
}
