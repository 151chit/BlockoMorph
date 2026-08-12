package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.util.List;
import java.util.function.Function;

public class NeoForgeBlockRenderer implements PlatformBlockTesselator {
	private final FluidHelper fluidHelper = new FluidHelper();
	private final PoseStack dummyMatrix = new PoseStack();
	private ModelBlockRenderer renderer;
	private Function<ChunkSectionLayer, VertexConsumer> output;
	private List<BlockModelPart> partList;
	private RandomSource randomsource;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.renderer = GuiUtils.MC.getBlockRenderer().getModelRenderer();
		this.output = layer -> bufferSource.getForBlock(PlayerSectionLayer.byChunkType(layer));
		this.partList = new ObjectArrayList<>();
		this.randomsource = new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
		this.fluidHelper.init(bufferSource);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.randomsource.setSeed(state.getSeed(keyPos));
		model.collectParts(getter, keyPos, state, this.randomsource, this.partList);
		this.dummyMatrix.setIdentity();
		this.renderer.tesselateBlock(getter, this.partList, state, keyPos, this.dummyMatrix, this.output, true, OverlayTexture.NO_OVERLAY);
		this.partList.clear();
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
