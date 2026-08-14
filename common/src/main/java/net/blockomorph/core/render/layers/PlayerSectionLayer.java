package net.blockomorph.core.render.layers;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

//Cross-version copy
public enum PlayerSectionLayer {
	SOLID(4194304, false, RenderPipelines.SOLID, RenderType.solid()),
	CUTOUT(4194304, false, RenderPipelines.CUTOUT, RenderType.cutout()),
	TRANSLUCENT(786432, true, RenderPipelines.TRANSLUCENT, RenderType.translucentMovingBlock());

	final int baseBufSize;
	final boolean translucent;
	final RenderPipeline pipeline;
	final RenderType renderType;
	PlayerSectionLayer(int baseBufSize, boolean translucent, RenderPipeline pipeline, RenderType renderType) {
		this.baseBufSize = baseBufSize;
		this.translucent = translucent;
		this.pipeline = pipeline;
		this.renderType = renderType;
	}

	public boolean isTranslucent() {
		return this.translucent;
	}

	public int baseBufferSize() {
		return this.baseBufSize;
	}

	public RenderPipeline pipeline() {
		return this.pipeline;
	}

	public RenderType renderType() {
		return this.renderType;
	}

	public static PlayerSectionLayer byChunkType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> SOLID;
			case CUTOUT, CUTOUT_MIPPED -> CUTOUT;
			case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT;
		};
	}
}
