package net.blockomorph.platformImpl.rendering;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("UnstableApiUsage")
public class FabricBlockRenderer implements PlatformBlockTesselator {
	private static final Renderer RENDERER_GETTER = IndigoRenderer.INSTANCE; //optimizers may give broken NonTerrain ctx
	private final FluidHelper fluidHelper = new FluidHelper();
	private AltModelBlockRenderer renderer;
	private QuadEmitter emitter;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.renderer = RENDERER_GETTER.altModelBlockRenderer(GuiUtils.MC.options.ambientOcclusion().get(), true, GuiUtils.MC.getBlockColors());
		this.emitter = RENDERER_GETTER.quadEmitter(quad ->
			quad.buffer(OverlayTexture.NO_OVERLAY, bufferSource.getForBlock(PlayerSectionLayer.byChunkType(quad.chunkLayer())))
		);
		this.fluidHelper.init(bufferSource);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.renderer.tesselateBlock(this.emitter, 0, 0, 0, getter, keyPos, state, model, state.getSeed(keyPos));
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
