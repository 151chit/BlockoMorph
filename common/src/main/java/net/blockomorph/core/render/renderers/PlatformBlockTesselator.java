package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

public interface PlatformBlockTesselator {
	default void initRenderer(boolean AO, BlockColors colors, Function<PlayerSectionLayer, VertexConsumer> callback) {}
	void tessellateBlock(BlockStateModel model,
	                     InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state);
	default void tessellateFluid(FluidRenderer fluidRenderer, FluidRenderer.Output output, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		fluidRenderer.tesselate(getter, keyPos, output, state, state.getFluidState());
	}
}
