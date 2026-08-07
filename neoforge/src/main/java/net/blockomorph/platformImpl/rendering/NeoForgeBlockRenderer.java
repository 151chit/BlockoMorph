package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.CustomFluidRenderer;

import java.util.function.Function;

public class NeoForgeBlockRenderer implements PlatformBlockTesselator {
	private ModelBlockRenderer renderer;
	private BlockQuadOutput output;
	private FluidState fluidState;
	private CustomFluidRenderer fluidRenderer;

	@Override
	public void initRenderer(boolean AO, BlockColors colors, Function<PlayerSectionLayer, VertexConsumer> callback) {
		this.renderer = new ModelBlockRenderer(AO, true, colors);
		this.output = (x, y, z, quad, settings) ->
			callback.apply(PlayerSectionLayer.byChunkType(quad.materialInfo().layer())).putBlockBakedQuad(x, y, z, quad, settings);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.renderer.tesselateBlock(this.output,
				getter.getAxisOffset(Direction.Axis.X, keyPos), getter.getAxisOffset(Direction.Axis.Y, keyPos), getter.getAxisOffset(Direction.Axis.Z, keyPos),
				getter, keyPos, state, model, state.getSeed(keyPos));
	}

	@Override
	public void tessellateFluid(FluidRenderer fluidRenderer, FluidRenderer.Output output, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		if (state.getFluidState() != this.fluidState) {
			this.fluidState = state.getFluidState();
			this.fluidRenderer = GuiUtils.MC.getModelManager().getFluidStateModelSet().get(this.fluidState).customRenderer();
		}
		if (this.fluidRenderer != null) {
			this.fluidRenderer.renderFluid(fluidRenderer, state.getFluidState(), getter, keyPos, output, state);
			return;
		}
		PlatformBlockTesselator.super.tessellateFluid(fluidRenderer, output, getter, keyPos, state);
	}
}
