package net.blockomorph.core.phys;

import com.google.common.collect.ImmutableList;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.function.BiConsumer;

public class MorphedCollisionGetter {

	public static void appendEntityCollisions(List<VoxelShape> entityCollisions, EntityGetter world, Entity entity, AABB box) {
		if (checkAndSkipIfNeed(box, entity)) return;
		collect(entityCollisions, List::add, world, entity, box);
	}

	public static List<VoxelShape> appendAsImmutable(List<VoxelShape> shapes, EntityGetter world, Entity self, AABB aabb) {
		if (checkAndSkipIfNeed(aabb, self)) return shapes;
		ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();
		builder.addAll(shapes);
		collect(builder, ImmutableList.Builder::add, world, self, aabb);
		return builder.build();
	}

	private static boolean checkAndSkipIfNeed(AABB box, Entity entity) {
		return box.getSize() < 1.0E-7D || entity instanceof Projectile && Config.get().hitReaction.getValue().projectile;
	}

	private static <T> void collect(T entityCollisions, BiConsumer<T, VoxelShape> adder, EntityGetter world, Entity entity, AABB box) {
		PlayersStorage storage = PlayersStorage.ofLevel(world);
		try (storage) {
			double inflate = 1.0E-7 + 1;
			double minX = box.minX - inflate;
			double minY = box.minY - inflate;
			double minZ = box.minZ - inflate;
			double maxX = box.maxX + inflate;
			double maxY = box.maxY + inflate;
			double maxZ = box.maxZ + inflate;
			CollisionContext ctx = entity != null ? CollisionContext.of(entity) : CollisionContext.empty();
			MutableObject<VoxelShape> playerShape = new MutableObject<>();
			for (Player player : storage.findMorphedPlayers(entity, minX, minY, minZ, maxX, maxY, maxZ)) {
				var cursor = PlayerAccessor.of(player).getManager().getCursor3D();
				try (cursor) {
					for (BlockInPlayer2 block : cursor.forAllBlocks(minX, minY, minZ, maxX, maxY, maxZ)) {
						handleBlockAndFillList(box, block, playerShape, entityCollisions, adder, ctx);
					}
				}
			}
		}
	}

	private static <T> void handleBlockAndFillList(AABB collisionBox, BlockInPlayer2 block, MutableObject<VoxelShape> playerShape,
			T entityCollisions, BiConsumer<T, VoxelShape> adder, CollisionContext ctx) {
		VoxelShape shape = block.getBlockState().getCollisionShape(block.getOwner().level(), block.getPos(), ctx);
		if (shape.isEmpty()) return;
		double x = MorphMath.getRealBlockPosAxis(Direction.Axis.X, block);
		double y = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block);
		double z = MorphMath.getRealBlockPosAxis(Direction.Axis.Z, block);
		if (!handleIntersectsWithCube(collisionBox, shape, x, y, z, entityCollisions, adder)) {
			double epsilon = 1.0E-7D;
			if (!collisionBox.intersects(
					shape.min(Direction.Axis.X) + x - epsilon,
					shape.min(Direction.Axis.Y) + y - epsilon,
					shape.min(Direction.Axis.Z) + z - epsilon,
					shape.max(Direction.Axis.X) + x + epsilon,
					shape.max(Direction.Axis.Y) + y + epsilon,
					shape.max(Direction.Axis.Z) + z + epsilon)) {
				return;
			}
			VoxelShape current = shape.move(x, y, z);
			if (playerShape.getValue() == null) {
				playerShape.setValue(Shapes.create(collisionBox));
			}
			if (Shapes.joinIsNotEmpty(current, playerShape.getValue(), BooleanOp.AND)) {
				adder.accept(entityCollisions, current);
			}
		}
	}

	private static <T> boolean handleIntersectsWithCube(AABB collisionBox, VoxelShape shape, double realBlockX, double realBlockY, double realBlockZ,
			T entityCollisions, BiConsumer<T, VoxelShape> adder) {
		if (shape == Shapes.block()) {
			if (collisionBox.intersects(realBlockX, realBlockY, realBlockZ, realBlockX + 1, realBlockY + 1, realBlockZ + 1)) {
				adder.accept(entityCollisions, shape.move(realBlockX, realBlockY, realBlockZ));
			}
			return true;
		}
		return false;
	}
}
