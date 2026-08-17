package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.function.Function;

public class NeoForgeBlockRenderer implements PlatformBlockTesselator {
	private final FluidHelper fluidHelper = new FluidHelper();
	private final PoseStack dummyMatrix = new PoseStack();
	private ModelBlockRenderer renderer;
	private Function<RenderType, VertexConsumer> output;
	private RandomSource randomsource;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.renderer = GuiUtils.MC.getBlockRenderer().getModelRenderer();
		this.output = layer -> bufferSource.getForBlock(PlayerSectionLayer.byChunkType(layer));
		this.randomsource = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
		this.fluidHelper.init(bufferSource);
	}

	@Override
	public void tessellateBlock(BakedModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		ModelData modelData = getter.getModelData(keyPos);
		modelData = model.getModelData(getter, keyPos, state, modelData);
		this.randomsource.setSeed(state.getSeed(keyPos));
		for(RenderType renderType : model.getRenderTypes(state, this.randomsource, modelData)) {
			VertexConsumer buffer = this.output.apply(renderType);
			this.dummyMatrix.setIdentity();
			this.renderer.tesselateBlock(getter, model, state, keyPos, this.dummyMatrix, buffer, true,
					this.randomsource, state.getSeed(keyPos), OverlayTexture.NO_OVERLAY, modelData, renderType);
		}
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
