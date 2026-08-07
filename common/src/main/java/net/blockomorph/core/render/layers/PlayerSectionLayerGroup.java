package net.blockomorph.core.render.layers;

import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

public enum PlayerSectionLayerGroup {
	OPAQUE(PlayerSectionLayer.SOLID, PlayerSectionLayer.CUTOUT),
	TRANSLUCENT(PlayerSectionLayer.TRANSLUCENT);

	final PlayerSectionLayer[] layers;

	PlayerSectionLayerGroup(final PlayerSectionLayer... layers) {
		this.layers = layers;
	}

	public PlayerSectionLayer[] layers() {
		return this.layers;
	}

	public ChunkSectionLayerGroup chunkType() {
		return switch (this) {
			case OPAQUE -> ChunkSectionLayerGroup.OPAQUE;
			case TRANSLUCENT -> ChunkSectionLayerGroup.TRANSLUCENT;
		};
	}

	public static PlayerSectionLayerGroup byChunkType(ChunkSectionLayerGroup layer) {
		return switch (layer) {
			case OPAQUE -> OPAQUE;
			case TRANSLUCENT -> TRANSLUCENT;
		};
	}
}
