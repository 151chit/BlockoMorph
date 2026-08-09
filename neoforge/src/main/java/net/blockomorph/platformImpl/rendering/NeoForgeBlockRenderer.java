package net.blockomorph.platformImpl.rendering;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.CustomFluidRenderer;

public class NeoForgeBlockRenderer implements PlatformBlockTesselator {
	private final FluidHelper fluidHelper = new FluidHelper();
	private ModelBlockRenderer renderer;
	private BlockQuadOutput output;
	private FluidState fluidState;
	private CustomFluidRenderer fluidRenderer;

	@Override
	public void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {
		this.renderer = new ModelBlockRenderer(GuiUtils.MC.options.ambientOcclusion().get(), true, GuiUtils.MC.getBlockColors());
		this.output = (x, y, z, quad, settings) ->
			bufferSource.getForBlock(PlayerSectionLayer.byChunkType(quad.materialInfo().layer())).putBlockBakedQuad(x, y, z, quad, settings);
		this.fluidHelper.init(bufferSource);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.renderer.tesselateBlock(this.output,
				getter.getAxisOffset(Direction.Axis.X, keyPos), getter.getAxisOffset(Direction.Axis.Y, keyPos), getter.getAxisOffset(Direction.Axis.Z, keyPos),
				getter, keyPos, state, model, state.getSeed(keyPos));
	}

	@Override
	public void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		if (state.getFluidState() != this.fluidState) {
			this.fluidState = state.getFluidState();
			this.fluidRenderer = GuiUtils.MC.getModelManager().getFluidStateModelSet().get(this.fluidState).customRenderer();
		}
		if (this.fluidRenderer != null) {
			this.fluidRenderer.renderFluid(this.fluidHelper.getRenderer(), state.getFluidState(), getter, keyPos, this.fluidHelper.getOutput(), state);
			return;
		}
		this.fluidHelper.tessellate(getter, keyPos, state);
	}
}
