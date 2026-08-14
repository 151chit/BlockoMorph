package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings({"OverrideOnly", "UnstableApiUsage"})
public class FabricBlockRenderer implements PlatformBlockTesselator {
	private static final Renderer RENDERER_GETTER = IndigoRenderer.INSTANCE; //optimizers may give broken NonTerrain ctx
	private final FluidHelper fluidHelper = new FluidHelper();
	private final PoseStack dummyMatrix = new PoseStack();
	private ModelBlockRenderer renderer;
	private MultiBufferSource output;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.renderer = GuiUtils.MC.getBlockRenderer().getModelRenderer();
		this.output = layer -> bufferSource.getForBlock(PlayerSectionLayer.byChunkType(layer));
		this.fluidHelper.init(bufferSource);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.dummyMatrix.setIdentity();
		RENDERER_GETTER.render(this.renderer, getter, model, state, keyPos, this.dummyMatrix, this.output, true, state.getSeed(keyPos), OverlayTexture.NO_OVERLAY);
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
