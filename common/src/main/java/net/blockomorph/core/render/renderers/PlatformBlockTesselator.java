package net.blockomorph.core.render.renderers;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface PlatformBlockTesselator {
	default void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {}
	void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state);
	void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state);

	class FluidHelper {
		private FluidRenderer.Output output;
		private FluidRenderer renderer;

		public FluidRenderer getRenderer() {
			return this.renderer;
		}

		public FluidRenderer.Output getOutput() {
			return this.output;
		}

		public void init(BakedBlocksRenderer.BufferSource bufferSource) {
			this.renderer = new FluidRenderer(GuiUtils.MC.getModelManager().getFluidStateModelSet());
			this.output = layer -> bufferSource.getForFluid(PlayerSectionLayer.byChunkType(layer));
		}

		public void tessellate(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
			this.renderer.tesselate(getter, keyPos, this.output, state, state.getFluidState());
		}
	}
}
