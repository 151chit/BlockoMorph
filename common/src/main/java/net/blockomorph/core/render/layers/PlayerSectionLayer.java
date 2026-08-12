package net.blockomorph.core.render.layers;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

//Cross-version copy
public enum PlayerSectionLayer {
	SOLID(4194304, false),
	CUTOUT(4194304, false),
	TRANSLUCENT(786432, true);

	final int baseBufSize;
	final boolean translucent;
	PlayerSectionLayer(int baseBufSize, boolean translucent) {
		this.baseBufSize = baseBufSize;
		this.translucent = translucent;
	}

	public boolean isTranslucent() {
		return this.translucent;
	}

	public int baseBufferSize() {
		return this.baseBufSize;
	}

	public ChunkSectionLayer chunkType() {
		return switch (this) {
			case SOLID -> ChunkSectionLayer.SOLID;
			case CUTOUT -> ChunkSectionLayer.CUTOUT;
			case TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
		};
	}

	public static PlayerSectionLayer byChunkType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> SOLID;
			case CUTOUT, CUTOUT_MIPPED -> CUTOUT;
			case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT;
		};
	}
}
