package net.blockomorph.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MovementCalculator {
    private final Player player;
    private final Level level;
    private final Direction lookDirection;
    private final boolean error;

    public MovementCalculator(PlayerAccessor playerAccessor) {
        this.player = (Player) playerAccessor;
        this.level = player.level();
        this.lookDirection = player.getDirection();
        this.error = this.lookDirection == Direction.DOWN || this.lookDirection == Direction.UP;
    }

    public void calculateEnterCorrection(Vec3 originalMovement) {
        double d0 = 1.0E-7D + 0.00001;
        if (this.error || (originalMovement.x == 0 && originalMovement.z == 0)) return;

        AABB main = null;
        Vec3 movement = new Vec3(
                d0 * (lookDirection.getStepX()),
                0,
                d0 * (lookDirection.getStepZ())
        );
        for (VoxelShape shp : level.getCollisions(player, player.getBoundingBox().move(movement))) {
            for (AABB aabb : shp.toAabbs()) {
                if (this.player.getBoundingBox().move(movement).intersects(aabb) && (main == null || this.isCloser(aabb, main, originalMovement))) {
                    main = aabb;
                }
            }
        }
        if (main == null) return;
        Vec3 offset = this.getHitBoxOffset(main, originalMovement);
        if (level.noCollision(player.getBoundingBox().move(offset).move(movement))) {
            if (this.isGoodPath(main, originalMovement)) {//broken
                player.setPos(offset.x + player.getX(), player.getY(), offset.z + player.getZ());
            }
        }
    }

    private boolean isGoodPath(AABB b, Vec3 m) {
        Direction dir = this.lookDirection;
        AABB playerBox = this.player.getBoundingBox();
        if (dir == Direction.NORTH) {
            if (m.x > 0)
                return playerBox.minX + m.x > b.maxX;
            return playerBox.maxX + m.x < b.minX;
        } else if (dir == Direction.SOUTH) {
            if (m.x < 0)
                return playerBox.maxX + m.x < b.minX;
            return playerBox.minX + m.x > b.maxX;
        } else if (dir == Direction.WEST) {
            if (m.z > 0)
                return playerBox.minZ + m.z > b.maxZ;
            return playerBox.maxZ + m.z < b.minZ;
        } else if (dir == Direction.EAST) {
            if (m.z < 0)
                return playerBox.maxZ + m.z < b.minZ;
            return playerBox.minZ + m.z > b.maxZ;
        }
        throw new IllegalArgumentException("Wrong side: " + dir);
    }

    private boolean isCloser(AABB a, AABB b, Vec3 m) {
        Direction dir = this.lookDirection;
        boolean flag = false;
        if (dir == Direction.NORTH) {
            flag = a.maxX < b.maxX;
            if (m.x > 0) flag = !flag;
        } else if (dir == Direction.SOUTH) {
            flag = a.minX > b.minX;
            if (m.x < 0) flag = !flag;
        } else if (dir == Direction.WEST) {
            flag = a.maxZ < b.maxZ;
            if (m.z > 0) flag = !flag;
        } else if (dir == Direction.EAST) {
            flag = a.minZ > b.minZ;
            if (m.z < 0) flag = !flag;
        }
        return flag;
    }

    private Vec3 getHitBoxOffset(AABB main, Vec3 m) {
        Direction dir = this.lookDirection;
        AABB playerBox = this.player.getBoundingBox();
        if (dir == Direction.NORTH) {
            if (m.x > 0) return new Vec3(main.maxX - playerBox.minX, 0, 0);
            return new Vec3(main.minX - playerBox.maxX, 0, 0);
        } else if (dir == Direction.SOUTH) {
            if (m.x < 0) return new Vec3(main.minX - playerBox.maxX, 0, 0);
            return new Vec3(main.maxX - playerBox.minX, 0, 0);
        } else if (dir == Direction.WEST) {
            if (m.z > 0) return new Vec3(0, 0, main.maxZ - playerBox.minZ);
            return new Vec3(0, 0, main.minZ - playerBox.maxZ);
        } else if (dir == Direction.EAST) {
            if (m.z < 0) return new Vec3(0, 0, main.minZ - playerBox.maxZ);
            return new Vec3(0, 0, main.maxZ - playerBox.minZ);
        }
        throw new IllegalArgumentException("Wrong side: " + dir);
    }
}
