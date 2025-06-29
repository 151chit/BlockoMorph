package net.blockomorph.utils.coords;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class InPlayerBlockPos {
	public static final int X_CHUNK_START = 60_000_000;//-134_217_738 + 3000;
	public static final int X_CHUNK_END = 120_000_000;//X_CHUNK_START - 30;
	public static final int X_CENTER = 90_000_000;
	public static final int Y_CHUNK_START = 64;
	public static final InPlayerBlockPos ZERO = InPlayerBlockPos.get(0, 0, 0);
	public final int x;
	public final int y;
	public final int z;

	public static boolean isMorphedPlayerX(double x) {
		return x >= (double) X_CHUNK_START && x <= (double) X_CHUNK_END;
	}

	//Real ChunkPos
	public static boolean isMorphPlayerChunk(ChunkPos pos) {
		return pos.getMinBlockX() > X_CHUNK_START && pos.getMaxBlockX() < X_CHUNK_END;
	}

	private InPlayerBlockPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public boolean isValid() {
		return this.x < 15 && this.x > -31 && this.z < 15 && this.z > -15 && this.y < 31 && this.y >= 0;
	}

	public InPlayerBlockPos offset(int x, int y, int z) {
		return InPlayerBlockPos.get(this.x + x, this.y + y, this.z + z);
	}

	public static Vec3 checkOnReal(Vec3 vec, @Nullable Vec3 detectionCoord) {
		Vec3 detectingPos = detectionCoord == null ? vec : detectionCoord;
		if (isMorphedPlayerX(vec.x) || detectionCoord != null) {
			PlayerMorphedSection pos = BlockPosBounds.getPlayerSectionPos(BlockPos.containing(detectingPos));
			Player player = BlockPosBounds.getPlayerByChunkPos(pos, null);
			if (player != null) {
				//double y = vec.y - pos.x * PlayerMorphedSection.MAX_SIZE - BlockPosBounds.WORLD_MIN - BlockPosBounds.WORLD_MIN;
				//double z = vec.z - pos.z * PlayerMorphedSection.MAX_SIZE - (double) PlayerMorphedSection.MAX_SIZE / 2 + 0.5 - BlockPosBounds.WORLD_MIN;
				//double x = vec.x - X_CHUNK_END - (double) PlayerMorphedSection.MAX_SIZE / 2 + 0.5;
				double d0 = 0.5d;
				double y = vec.y - Y_CHUNK_START;
				double x = vec.x - pos.x * PlayerMorphedSection.FOR_TWO_CHUNKS - (double)PlayerMorphedSection.MAX_SIZE / 2 - X_CHUNK_START + d0;
				double z = vec.z - pos.z * PlayerMorphedSection.FOR_TWO_CHUNKS - (double)PlayerMorphedSection.MAX_SIZE / 2 + d0;
				return MorphUtils.getRealBlockPos(PlayerAccessor.of(player), new Vec3(x, y, z));
			}
		}
		return vec;
	}

	public static Vec3 checkOnReal(Vec3 vec) {
		return checkOnReal(vec, null);
	}

	@Nullable
	public BlockPos boundedBlockPos(Player player) {
		if (player != null) {
			PlayerMorphedSection pos = BlockPosBounds.getChunkPosForPlayer(player);
			if (pos != null) {
				int y = Y_CHUNK_START + this.y;
				int x = X_CHUNK_START + pos.x * PlayerMorphedSection.FOR_TWO_CHUNKS + this.x + PlayerMorphedSection.MAX_SIZE / 2;
				int z = pos.z * PlayerMorphedSection.FOR_TWO_CHUNKS + PlayerMorphedSection.MAX_SIZE / 2 + this.z;
				return new BlockPos(x, y, z);
			}
		}
		return null;
	}

	public static AABB checkOnReal(AABB aabb) {
		if ((aabb.minX > X_CHUNK_START || aabb.maxX > X_CHUNK_START)) {
			Vec3 center = aabb.getCenter();
			Vec3 min = checkOnReal(new Vec3(aabb.minX, aabb.minY, aabb.minZ), center);
			Vec3 max = checkOnReal(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ), center);
			return new AABB(min, max);
		}
		return aabb;
	}

	public static BlockPos checkOnReal(BlockPos pos) {
		return BlockPos.containing(InPlayerBlockPos.checkOnReal(pos.getCenter()));
	}

	public static InPlayerBlockPos get(int x, int y, int z) {
		return new InPlayerBlockPos(x, y, z);
	}

	public static void check(BlockPos bounded, BiConsumer<PlayerAccessor, InPlayerBlockPos> action, Runnable elseRun, @Nullable Boolean lv) {
		if (bounded != null && action != null && isMorphedPlayerX(bounded.getX())) {
			PlayerMorphedSection pos = BlockPosBounds.getPlayerSectionPos(bounded);
			Player player = BlockPosBounds.getPlayerByChunkPos(pos, lv);
			if (player != null) {
				int y = bounded.getY() - Y_CHUNK_START;
				int x = bounded.getX() - pos.x * PlayerMorphedSection.FOR_TWO_CHUNKS - PlayerMorphedSection.MAX_SIZE / 2 - X_CHUNK_START;
				int z = bounded.getZ() - pos.z * PlayerMorphedSection.FOR_TWO_CHUNKS - PlayerMorphedSection.MAX_SIZE / 2;
				action.accept(PlayerAccessor.of(player), new InPlayerBlockPos(x, y, z));
			} else if (elseRun != null) {
				elseRun.run();
			}
		}
	}

	public static void check(BlockPos bounded, BiConsumer<PlayerAccessor, InPlayerBlockPos> action, Runnable elseRun, LevelReader lv) {
		check(bounded, action, elseRun, lv.isClientSide());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof InPlayerBlockPos bl)
			return bl.x == this.x && bl.y == this.y && bl.z == this.z;
		return false;
	}

	@Nullable
	public static InPlayerBlockPos parseBlockPos(String input) {
		String[] parts = input.trim().split(" ");
		if (parts.length != 3)
			return null;
		try {
			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			int z = Integer.parseInt(parts[2]);
			return new InPlayerBlockPos(x, y, z);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public FriendlyByteBuf write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.x);
		buffer.writeInt(this.y);
		buffer.writeInt(this.x);
		return buffer;
	}

	public InPlayerBlockPos(FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public String string() {
		return this.x + " " + this.y + " " + this.z;
	}

	@Override
	public String toString() {
		return "InPlayerBlockPos[x=" + this.x + " y=" + this.y + " z=" + this.z + "]";
	}

	@Override
	public int hashCode() {
		return (this.x + this.y * 29) * 29 + z;
	}
}
