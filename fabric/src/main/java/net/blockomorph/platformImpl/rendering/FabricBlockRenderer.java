package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

@SuppressWarnings("UnstableApiUsage")
public class FabricBlockRenderer extends BlockRenderContext implements PlatformBlockTesselator {
	private final FluidHelper fluidHelper = new FluidHelper();
	private final PoseStack dummyMatrix = new PoseStack();
	private MultiBufferSource output;
	private RandomSource randomsource;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.output = layer -> bufferSource.getForBlock(PlayerSectionLayer.byChunkType(layer));
		this.randomsource = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
		this.fluidHelper.init(bufferSource);
	}

	@Override
	protected VertexConsumer getVertexConsumer(RenderType layer) {
		return this.output.getBuffer(layer);
	}

	@Override
	public void tessellateBlock(BakedModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.dummyMatrix.setIdentity();
		this.render(getter, model, state, keyPos, this.dummyMatrix, null, true, this.randomsource, state.getSeed(keyPos), OverlayTexture.NO_OVERLAY);
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
