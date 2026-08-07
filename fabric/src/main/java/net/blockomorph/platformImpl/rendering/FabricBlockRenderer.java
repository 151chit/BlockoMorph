package net.blockomorph.platformImpl.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class FabricBlockRenderer implements PlatformBlockTesselator {
	private static final Renderer RENDERER_GETTER = IndigoRenderer.INSTANCE; //optimizers may give broken NonTerrain ctx
	private AltModelBlockRenderer renderer;
	private QuadEmitter emitter;

	@Override
	public void initRenderer(boolean AO, BlockColors colors, Function<PlayerSectionLayer, VertexConsumer> callback) {
		this.renderer = RENDERER_GETTER.altModelBlockRenderer(AO, true, colors);
		this.emitter = RENDERER_GETTER.quadEmitter(quad ->
			quad.buffer(OverlayTexture.NO_OVERLAY, callback.apply(PlayerSectionLayer.byChunkType(quad.chunkLayer())))
		);
	}

	@Override
	public void tessellateBlock(BlockStateModel model, InPlayerBlockAndTintGetter getter, BlockPos keyPos, BlockState state) {
		this.renderer.tesselateBlock(this.emitter,
				getter.getAxisOffset(Direction.Axis.X, keyPos), getter.getAxisOffset(Direction.Axis.Y, keyPos), getter.getAxisOffset(Direction.Axis.Z, keyPos),
				getter, keyPos, state, model, state.getSeed(keyPos));
	}
}
