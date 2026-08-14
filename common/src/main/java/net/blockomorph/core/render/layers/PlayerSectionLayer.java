package net.blockomorph.core.render.layers;

import net.minecraft.client.renderer.RenderType;

//Cross-version copy
public enum PlayerSectionLayer {
	SOLID(4194304, false, RenderType.solid(), RenderType.solid()),
	CUTOUT(4194304, false, RenderType.cutout(), RenderType.cutout()),
	TRANSLUCENT(786432, true, RenderType.translucent(), RenderType.translucentMovingBlock());

	final int baseBufSize;
	final boolean translucent;
	final RenderType pipeline;
	final RenderType renderType;
	PlayerSectionLayer(int baseBufSize, boolean translucent, RenderType pipeline, RenderType renderType) {
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

	public RenderType pipeline() {
		return this.pipeline;
	}

	public RenderType renderType() {
		return this.renderType;
	}

	public static PlayerSectionLayer byChunkType(RenderType layer) {
		if (layer == RenderType.solid()) {
			return SOLID;
		} else if (layer == RenderType.cutout() || layer == RenderType.cutoutMipped()) {
			return CUTOUT;
		}
		return TRANSLUCENT;
	}
}
