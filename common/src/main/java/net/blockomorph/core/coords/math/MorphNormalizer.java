package net.blockomorph.core.coords.math;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.utils.accessors.Accessors;
import net.blockomorph.utils.side.ThreadLocalMutableBlockPos;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MorphNormalizer {

	public static double normalize(Direction.Axis axis, double morphCoordCandidate, double targetX, double targetZ) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(targetX)) {
			long section = MorphedPlayerSection.fromMorphedBlockPos(Mth.floor(targetX), Mth.floor(targetZ));
			if (section != -1) {
				Player pl = BlockPosBounds.getPlayerBySection(section);
				if (pl != null) {
					double result = switch (axis) {
						case Y -> morphCoordCandidate - InPlayerBlockPos.Y_CHUNK_START;
						case X -> morphCoordCandidate - MorphedPlayerSection.getX(section) * MorphedPlayerSection.FOR_TWO_CHUNKS
								- (double) MorphedPlayerSection.MAX_PLAYER_SIZE / 2 - InPlayerBlockPos.X_CHUNK_START + 0.5;
						case Z -> morphCoordCandidate - MorphedPlayerSection.getZ(section) * MorphedPlayerSection.FOR_TWO_CHUNKS
								- (double) MorphedPlayerSection.MAX_PLAYER_SIZE / 2 + 0.5;
					};
					return MorphMath.getRealBlockPosAxis(axis, PlayerAccessor.of(pl), result);
				}
			}
		}
		return morphCoordCandidate;
	}

	public static double normalizeOneOf(double x, double y, double z, Direction.Axis axis) {
		return normalize(axis, axis.choose(x, y, z), x, z);
	}

	public static double normalizeOneOf(Vec3 pos, Direction.Axis axis) {
		return normalizeOneOf(pos.x, pos.y, pos.z, axis);
	}

	public static double normalizeOneOf(Vec3i pos, Direction.Axis axis) {
		return normalizeOneOf(pos.getX(), pos.getY(), pos.getZ(), axis);
	}

	public static Vec3 normalize(Vec3 vec) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(vec.x)) return vec;
		double x = normalizeOneOf(vec, Direction.Axis.X);
		double y = normalizeOneOf(vec, Direction.Axis.Y);
		double z = normalizeOneOf(vec, Direction.Axis.Z);
		return new Vec3(x, y, z);
	}

	public static BlockPos normalize(BlockPos orig, ThreadLocalMutableBlockPos mutable) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(orig.getX())) {
			var blockpos = mutable.set(orig.getX(), orig.getY(), orig.getZ());
			if (blockpos != null) {
				blockpos.set(
					Mth.floor(normalizeOneOf(orig, Direction.Axis.X)),
					Mth.floor(normalizeOneOf(orig, Direction.Axis.Y)),
					Mth.floor(normalizeOneOf(orig, Direction.Axis.Z))
				);
				return blockpos;
			}
		}
		return orig;
	}

	public static double tryNormalizeAabbAxis(AABB orig, Direction.Axis axis, Direction.AxisDirection dir) {
		double centerX = Mth.lerp(0.5, orig.minX, orig.maxX);
		double centerY = Mth.lerp(0.5, orig.minY, orig.maxY);
		double centerZ = Mth.lerp(0.5, orig.minZ, orig.maxZ);
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(centerX)) {
			if (dir == Direction.AxisDirection.POSITIVE)
				return axis.choose(orig.maxX, orig.maxY, orig.maxZ);
			else return axis.choose(orig.minX, orig.minY, orig.minZ);
		}
		double norm = normalizeOneOf(centerX, centerY, centerZ, axis);
		double part = axis.choose(orig.getXsize(), orig.getYsize(), orig.getZsize()) / 2;
		return dir == Direction.AxisDirection.POSITIVE ? norm + part : norm - part;
	}

	public static AABB tryNormalizeAabb(AABB orig) {
		double centerX = Mth.lerp(0.5, orig.minX, orig.maxX);
		double centerY = Mth.lerp(0.5, orig.minY, orig.maxY);
		double centerZ = Mth.lerp(0.5, orig.minZ, orig.maxZ);
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(centerX)) return orig;

		double normCenterX = normalizeOneOf(centerX, centerY, centerZ, Direction.Axis.X);
		double normCenterY = normalizeOneOf(centerX, centerY, centerZ, Direction.Axis.Y);
		double normCenterZ = normalizeOneOf(centerX, centerY, centerZ, Direction.Axis.Z);

		double xPart = orig.getXsize() / 2;
		double yPart = orig.getYsize() / 2;
		double zPart = orig.getZsize() / 2;
		return new AABB(normCenterX - xPart, normCenterY - yPart, normCenterZ - zPart,
				normCenterX + xPart, normCenterY + yPart, normCenterZ + zPart);
	}

	public static void normalizeEntityPos(Entity entity, double x, double y, double z) {
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(x)) return;
		entity.setPosRaw(
				normalizeOneOf(x, y, z, Direction.Axis.X),
				normalizeOneOf(x, y, z, Direction.Axis.Y),
				normalizeOneOf(x, y, z, Direction.Axis.Z)
		);
		entity.refreshDimensions();
	}

	public static void normalizeEntityPos(Entity entity) {
		normalizeEntityPos(entity, entity.getX(), entity.getY(), entity.getZ());
	}

	public static void normalizeParticlePos(Particle p) {
		double x = Accessors.ParticleAccessor.of(p).getCoords$bm(Direction.Axis.X);
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			double y = Accessors.ParticleAccessor.of(p).getCoords$bm(Direction.Axis.Y);
			double z = Accessors.ParticleAccessor.of(p).getCoords$bm(Direction.Axis.Z);
			p.setPos(
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.X),
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Y),
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Z)
			);
		}
	}

	public static ServerPlayer.RespawnConfig normalizeSpawnpoint(ServerPlayer.RespawnConfig cfg) {
		if (cfg == null) return null;
		var data = cfg.respawnData();
		BlockPos orig = data.pos();
		if (!InPlayerBlockPos.isMorphedPlayerBlockX(orig.getX())) return cfg;
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(orig);
		if (pl == null) return null;
		Vec3 normalized = normalize(Vec3.atBottomCenterOf(orig));
		LevelData.RespawnData newData = new LevelData.RespawnData(GlobalPos.of(pl.player().level().dimension(), BlockPos.containing(normalized)), data.yaw(), data.pitch());
		return new ServerPlayer.RespawnConfig(newData, true);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static boolean normalizePacketSendPosAndSend(List<ServerPlayer> players, int playerIdExclude,
	                                                    double x, double y, double z, double range, ResourceKey<Level> targetDimension, Packet<?> packet) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			for (int i = 0; i < players.size(); i++) {
				ServerPlayer player = players.get(i);
				if (player.getId() != playerIdExclude && player.level().dimension() == targetDimension) {
					if (MorphDistanceTo.forPoints(Double.MAX_VALUE, player.position(), x, y, z, true, 0) < range * range) {
						player.connection.send(packet);
					}
				}
			}
			return false;
		}
		return true;
	}
}
