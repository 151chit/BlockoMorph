package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Function;

public interface PlatformBlockTesselator {
	default void initRenderer(BakedBlocksRenderer.BufferSource bufferSource) {}
	void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state);
	void tessellateFluid(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state);

	class FluidHelper {
		private Function<ChunkSectionLayer, VertexConsumer> output;
		private LiquidBlockRenderer renderer;
		private FluidState currentFluidState;
		private ChunkSectionLayer liquidLayer;

		public void init(BakedBlocksRenderer.BufferSource bufferSource) {
			this.renderer = Accessors.BlockRendererDispatcherAccessor.getFluidRenderer();
			this.output = layer -> bufferSource.getForFluid(PlayerSectionLayer.byChunkType(layer));
		}

		public void tessellate(InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
			if (this.currentFluidState != state.getFluidState()) {
				this.currentFluidState = state.getFluidState();
				this.liquidLayer = ItemBlockRenderTypes.getRenderLayer(this.currentFluidState);
			}
			this.renderer.tesselate(getter, keyPos, this.output.apply(this.liquidLayer), state, this.currentFluidState);
		}
	}
}
