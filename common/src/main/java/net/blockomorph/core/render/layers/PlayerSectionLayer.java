package net.blockomorph.core.render.layers;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.blockomorph.mixins.main.rawAccessors.RenderPipelinesAcc;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

//Cross-version copy
public enum PlayerSectionLayer {
	SOLID(4194304, false, RenderPipelines.SOLID_BLOCK, RenderTypes.solidMovingBlock()),
	CUTOUT(4194304, false, RenderPipelines.CUTOUT_BLOCK, RenderTypes.cutoutMovingBlock()),
	TRANSLUCENT(786432, true, RenderPipelinesAcc.reg$bm(
			RenderPipeline.builder(RenderPipelinesAcc.blockSnippet$bm())
			.withLocation(GuiUtils.res("pipeline/translucent_block"))
			.withShaderDefine("ALPHA_CUTOUT", 0.01F)
			.withBlend(BlendFunction.TRANSLUCENT)
			.build()), RenderTypes.translucentMovingBlock());

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
			case CUTOUT -> CUTOUT;
			case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT;
		};
	}
}