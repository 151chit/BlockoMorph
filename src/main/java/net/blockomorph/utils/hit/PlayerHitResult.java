package net.blockomorph.utils.hit;

import net.blockomorph.utils.*;
import net.blockomorph.utils.accessors.ClipContextAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PlayerHitResult {
    public static final Predicate<Entity> NOT_MORPHED_PLAYER = (entity -> !(entity instanceof PlayerAccessor pl) || !pl.isFullActive());
    private static final AABB CUBE = Shapes.block().bounds();

    @Nullable
    public static MorphedPlayerHitResult calculateMorphedPlayerHitResult(Level lv, @Nullable Entity looker, Vec3 eyePosition, Vec3 reachVector, ClipContext.Block mode) {

        AABB areaBetweenAndReachEnd = new AABB(eyePosition, reachVector);
        List<Entity> entities = lv.getEntities(looker, areaBetweenAndReachEnd);

        MorphedPlayerHitResult result = null;
        double distanceToPartOfBlock = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (entity instanceof PlayerAccessor mob && mob.isFullActive()) {
                for (Map.Entry<InPlayerBlockPos, BlockInPlayer2> block : mob.getBlocksData2().entrySet()) {
                    BlockInPlayer2 bl = block.getValue();
                    InPlayerBlockPos offset = block.getKey();

                    Vec3 offsetPosInWorld = MorphUtils.getRealBlockPos(mob, offset);
                    AABB cubeAABB = CUBE.move(offsetPosInWorld);

                    if (cubeAABB.intersects(areaBetweenAndReachEnd)) {
                        VoxelShape blockShape = mode.get(bl.getBlockState(), lv, bl.getPos(), looker != null ? CollisionContext.of(looker) : CollisionContext.empty());
                        blockShape = blockShape.move(offsetPosInWorld.x, offsetPosInWorld.y, offsetPosInWorld.z);
                        for (AABB partBlockShape : blockShape.toAabbs()) {
                            Optional<Vec3> partHitResult = partBlockShape.clip(eyePosition, reachVector);
                            if (partHitResult.isPresent()) {
                                Vec3 res = partHitResult.get();
                                double dist = eyePosition.distanceTo(res);
                                if (dist < distanceToPartOfBlock || isMainBlock(result, res, offsetPosInWorld)) {
                                    distanceToPartOfBlock = dist;
                                    Direction dir = getClosestHitSide(blockShape, res);
                                    if (dir != null) {
                                        Vec3 inBlockOffset = new Vec3(res.x() - offsetPosInWorld.x, res.y() - offsetPosInWorld.y, res.z() - offsetPosInWorld.z);
                                        result = MorphedPlayerHitResult.of(
                                                mob,
                                                offset,
                                                dir,
                                                inBlockOffset.x == (int)inBlockOffset.x || inBlockOffset.y == (int)inBlockOffset.y ||inBlockOffset.z == (int)inBlockOffset.z,
                                                inBlockOffset,
                                                res
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
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

    @Nullable
    public static Direction getClosestHitSide(VoxelShape voxelShape, Vec3 hitPosition) {
        for (AABB aabb : voxelShape.toAabbs()) {
            if (containsAABB(aabb, hitPosition)) {
                return calculateHitDirection(hitPosition, aabb);
            }
        }

        return null;
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

    public static void checkHitResult(Vec3 oldHitPos, ClipContext ctx, Consumer<MorphedPlayerHitResult> ifGood) {
        CollisionContext collisionContext = ClipContextAccessor.of(ctx).getContext();
        if (collisionContext instanceof EntityCollisionContext context) {
            Entity looker = context.getEntity();
            if (looker != null) {
                Vec3 start = ctx.getFrom();
                MorphedPlayerHitResult hit = PlayerHitResult.calculateMorphedPlayerHitResult(looker.level(), looker, start, ctx.getTo(), ClipContextAccessor.of(ctx).getMode());
                if (hit != null && start.distanceTo(hit.getRealLocation()) < start.distanceTo(oldHitPos)) {
                    ifGood.accept(hit);
                }
            }
        }
    }
}
