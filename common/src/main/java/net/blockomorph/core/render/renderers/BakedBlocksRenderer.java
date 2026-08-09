package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.render.RenderingPlatformService;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.CompatAccessors;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;

import java.util.UUID;

public abstract class BakedBlocksRenderer {
	private final VertexDelegate fluidAdapter = new VertexDelegate() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			float xOffset = (float) (currentPos.getX() & 15);
			float yOffset = (float) (currentPos.getY() & 15);
			float zOffset = (float) (currentPos.getZ() & 15);
			return super.addVertex(
					x - xOffset + currentGetter.getAxisOffset(Direction.Axis.X, currentPos),
					y - yOffset + currentGetter.getAxisOffset(Direction.Axis.Y, currentPos),
					z - zOffset + currentGetter.getAxisOffset(Direction.Axis.Z, currentPos));
		}
	};
	private final VertexDelegate blockAdapter = new VertexDelegate() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return super.addVertex(
					x + currentGetter.getAxisOffset(Direction.Axis.X, currentPos),
					y + currentGetter.getAxisOffset(Direction.Axis.Y, currentPos),
					z + currentGetter.getAxisOffset(Direction.Axis.Z, currentPos));
		}
	};
	private final BufferSource bufferSource = new BufferSource() {
		@Override
		public VertexConsumer getForBlock(PlayerSectionLayer layer) {
			blockAdapter.setDelegate(getOutput(checkRenderType(layer)));
			return blockAdapter;
		}

		@Override
		public VertexConsumer getForFluid(PlayerSectionLayer layer) {
			fluidAdapter.setDelegate(getOutput(layer));
			return fluidAdapter;
		}
	};
	private final PlatformBlockTesselator dependModule = RenderingPlatformService.INSTANCE.createBlockTessellator();
	protected final UUID ownerId;
	private boolean cutOutLeaves;
	private BlockStateModelSet blockModels;
	private BlockStateModel currentModel;
	private BlockState currentState;
	private BlockPos currentPos;
	private InPlayerBlockAndTintGetter currentGetter;

	protected BakedBlocksRenderer(UUID playerID) {
		this.ownerId = playerID;
	}

	protected void renderBlocks(InPlayerBlockAndTintGetter inPlayerWorld) {
		this.checkRenderers();
		this.currentGetter = inPlayerWorld;
		this.currentGetter.forEachRenderBlocks((keyPos, blockState, renderFluid) -> {
			this.cacheForBlock(keyPos, blockState);
			if (blockState.getRenderShape() == RenderShape.MODEL) try {
				this.renderBlock();
			} catch (Exception e) {
				this.error(e, "block", this.currentState);
			}
			if (renderFluid && !blockState.getFluidState().isEmpty()) try {
				this.renderFluid();
			} catch (Exception e) {
				this.error(e, "fluid", this.currentState.getFluidState());
			}
			return !this.isInterrupted();
		});
		this.currentGetter = null;
		this.blockAdapter.setDelegate(null);
		this.fluidAdapter.setDelegate(null);
	}

	private void error(Exception e, String typeName, StateHolder<?, ?> type) {
		MorphUtils.LOGGER.error("When tessellating {} in player has error occurred. Player: {} Pos: {} Type: {}",
				typeName, this.ownerId, InPlayerBlockPos.decode(InPlayerBlockPos.fromDelta(this.currentGetter.getZeroKeyPos(), this.currentPos)), type, e);
	}

	private void checkRenderers() {
		this.blockModels = GuiUtils.MC.getModelManager().getBlockStateModelSet();
		this.cutOutLeaves = GuiUtils.MC.options.cutoutLeaves().get();
		this.dependModule.initRenderer(this.bufferSource);
	}

	private void renderBlock() {
		this.dependModule.tessellateBlock(this.currentModel, this.currentGetter, this.currentPos, this.currentState);
	}

	protected boolean isInterrupted() {
		return false;
	}

	protected abstract VertexConsumer getOutput(PlayerSectionLayer layer);

	private void cacheForBlock(BlockPos pos, BlockState state) {
		if (this.currentState != state) {
			this.currentModel = this.blockModels.get(state);
		}
		this.currentState = state;
		this.currentPos = pos;
	}

	public static RenderType layerToRenderType(PlayerSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderTypes.solidMovingBlock();
			case CUTOUT -> RenderTypes.cutoutMovingBlock();
			case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
		};
	}

	private PlayerSectionLayer checkRenderType(PlayerSectionLayer orig) {
		if (this.currentState.is(Blocks.REDSTONE_WIRE) ||
				this.currentState.is(Blocks.GLASS) ||
				this.currentState.is(Blocks.GLASS_PANE)
		) return PlayerSectionLayer.CUTOUT;
		if (!this.cutOutLeaves && this.currentState.getBlock() instanceof LeavesBlock)
			return PlayerSectionLayer.SOLID;
		return orig;
	}

	private void renderFluid() {
		this.activateSprites();
		this.currentGetter.noExternal(true);
		try {
			this.dependModule.tessellateFluid(this.currentGetter, this.currentPos, this.currentState);
		} finally {
			this.currentGetter.noExternal(false);
		}
	}

	private void activateSprites() {
		var sprites = RenderingPlatformService.INSTANCE.spritesForFluid(this.currentGetter, this.currentPos, this.currentState);
		for (TextureAtlasSprite sprite : sprites) {
			if (sprite != null && sprite.contents() instanceof CompatAccessors.SodiumSpriteActivator runner) {
				runner.activate$bm();
			}
		}
	}

	public interface BufferSource {
		VertexConsumer getForBlock(PlayerSectionLayer layer);
		VertexConsumer getForFluid(PlayerSectionLayer layer);
	}

	private static class VertexDelegate implements VertexConsumer {
		VertexConsumer original;

		void setDelegate(VertexConsumer consumer) {
			this.original = consumer;
		}

		void ensureNotEmpty() {
			if (this.original == null) throw new IllegalStateException("NotActive");
		}

		@Override public VertexConsumer addVertex(float x, float y, float z) { this.ensureNotEmpty(); this.original.addVertex(x, y, z); return this; }
		@Override public VertexConsumer setColor(int r, int g, int b, int a) { this.ensureNotEmpty(); this.original.setColor(r, g, b, a); return this; }
		@Override public VertexConsumer setColor(int color) { this.ensureNotEmpty(); this.original.setColor(color); return this; }
		@Override public VertexConsumer setUv(float u, float v) { this.ensureNotEmpty(); this.original.setUv(u, v); return this; }
		@Override public VertexConsumer setUv1(int u, int v) { this.ensureNotEmpty(); this.original.setUv1(u, v); return this; }
		@Override public VertexConsumer setUv2(int u, int v) { this.ensureNotEmpty(); this.original.setUv2(u, v); return this; }
		@Override public VertexConsumer setNormal(float x, float y, float z) { this.ensureNotEmpty(); this.original.setNormal(x, y, z); return this; }
		@Override public VertexConsumer setLineWidth(float width) { this.ensureNotEmpty(); this.original.setLineWidth(width); return this; }
	}
}