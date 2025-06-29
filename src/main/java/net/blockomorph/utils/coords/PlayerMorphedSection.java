package net.blockomorph.utils.coords;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayerMorphedSection {
	public static final int MAX_SIZE = 31;
	public static final int FOR_TWO_CHUNKS = 32;
	public final int x;
	public final int z;

	public PlayerMorphedSection(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public PlayerMorphedSection(long encoded) {
		this.x = (int)encoded;
		this.z = (int)(encoded >> 32);
	}

	@Nullable
	public static Player getPlayerByChunkPos(ChunkPos real, @Nullable Boolean client) {
		if (!InPlayerBlockPos.isMorphPlayerChunk(real)) return null;
		BlockPos bounded = new BlockPos(real.getBlockAt(7, 0, 7));
		PlayerMorphedSection section = BlockPosBounds.getPlayerSectionPos(bounded);
		return BlockPosBounds.getPlayerByChunkPos(section, client);
	}

	public long toLong() {
		return ChunkPos.asLong(this.x, this.z);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.x, this.z);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof PlayerMorphedSection section) {
			return section.x == this.x && section.z == this.z;
		}
		return false;
	}

	@Override
	public String toString() {
		return "PlayerMorphedSection[" + this.x + ", " + this.z + "]";
	}
}
