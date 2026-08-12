package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.render.utils.RenderingPlatformService;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.compat.AtlasSpriteFinder;
import net.blockomorph.utils.compat.CompatAccessors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public abstract class BakedBlocksRenderer {
	private final PlayerTransofrmerVertexConsumer fluidAdapter = new PlayerTransofrmerVertexConsumer() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			float xOffset = (float) (currentPos.getX() & 15);
			float yOffset = (float) (currentPos.getY() & 15);
			float zOffset = (float) (currentPos.getZ() & 15);
			return super.addVertex(x - xOffset, y - yOffset, z - zOffset);
		}
	};
	private final PlayerTransofrmerVertexConsumer blockAdapter = new PlayerTransofrmerVertexConsumer();
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
	private final Set<TextureAtlasSprite> collectedAnimatedSprites = new ObjectOpenHashSet<>(BlocksInPlayerStorage.ONE_AXIS);
	private final Set<TextureAtlasSprite> exportedCollectedSprites = Collections.unmodifiableSet(this.collectedAnimatedSprites);
	protected final UUID ownerId;
	private BlockModelShaper blockModels;
	private BlockStateModel currentModel;
	private BlockState currentState;
	private BlockPos currentPos;
	private InPlayerBlockAndTintGetter currentGetter;

	protected BakedBlocksRenderer(UUID playerID) {
		this.ownerId = playerID;
	}

	protected void renderBlocks(InPlayerBlockAndTintGetter inPlayerWorld) {
		this.collectedAnimatedSprites.clear();
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

	protected Set<TextureAtlasSprite> getCollectedAnimatedSprites() {
		return this.exportedCollectedSprites;
	}

	protected void activateSprite(TextureAtlasSprite sprite) {
		if (sprite.contents() instanceof CompatAccessors.SodiumSpriteActivator activator) {
			activator.activate$bm();
		}
	}

	protected void initOnMainThread() {
		this.blockAdapter.init();
		this.fluidAdapter.init();
	}

	private void error(Exception e, String typeName, StateHolder<?, ?> type) {
		MorphUtils.LOGGER.error("When tessellating {} in player has error occurred. Player: {} Pos: {} Type: {}",
				typeName, this.ownerId, InPlayerBlockPos.decode(InPlayerBlockPos.fromDelta(this.currentGetter.getZeroKeyPos(), this.currentPos)), type, e);
	}

	private void checkRenderers() {
		this.blockModels = GuiUtils.MC.getModelManager().getBlockModelShaper();
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
			this.currentModel = this.blockModels.getBlockModel(state);
		}
		this.currentState = state;
		this.currentPos = pos;
		this.blockAdapter.resetUv();
		this.fluidAdapter.resetUv();
	}

	public static RenderType layerToRenderType(PlayerSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderType.solid();
			case CUTOUT -> RenderType.cutout();
			case TRANSLUCENT -> RenderType.translucentMovingBlock();
		};
	}

	private PlayerSectionLayer checkRenderType(PlayerSectionLayer orig) {
		if (this.currentState.is(Blocks.REDSTONE_WIRE) ||
				this.currentState.is(Blocks.GLASS) ||
				this.currentState.is(Blocks.GLASS_PANE)
		) return PlayerSectionLayer.CUTOUT;
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
			this.addSpriteForTick(sprite);
		}
	}

	private void addSpriteForTick(TextureAtlasSprite sprite) {
		if (sprite != null && sprite.contents() instanceof CompatAccessors.SodiumSpriteActivator activator && activator.hasAnim$bm()) {
			this.collectedAnimatedSprites.add(sprite);
		}
	}

	public interface BufferSource {
		VertexConsumer getForBlock(PlayerSectionLayer layer);
		VertexConsumer getForFluid(PlayerSectionLayer layer);
	}

	private class PlayerTransofrmerVertexConsumer implements VertexConsumer {
		volatile AtlasSpriteFinder finder;
		int vertexCount;
		float u, v;
		VertexConsumer original;

		void setDelegate(VertexConsumer consumer) {
			this.original = consumer;
		}

		void init() {
			if (GuiUtils.MC.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS) instanceof AtlasSpriteFinder.Holder finderHolder) {
				this.finder = finderHolder.getOrMake();
				return;
			}
			this.finder = null;
		}

		void resetUv() {
			this.vertexCount = 0;
			this.u = 0;
			this.v = 0;
		}

		private void ensureNotEmpty() {
			if (this.original == null) throw new IllegalStateException("NotActive");
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			this.ensureNotEmpty();
			this.original.addVertex(x + currentGetter.getAxisOffset(Direction.Axis.X, currentPos),
					y + currentGetter.getAxisOffset(Direction.Axis.Y, currentPos),
					z + currentGetter.getAxisOffset(Direction.Axis.Z, currentPos));
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			this.ensureNotEmpty();
			this.original.setUv(u, v);
			this.u += u;
			this.v += v;
			this.vertexCount++;
			if (this.vertexCount == 4) {
				this.resetUv();
				if (this.finder != null)
					addSpriteForTick(this.finder.find(this.u * 0.25f, this.v * 0.25f));
			}
			return this;
		}

		@Override public VertexConsumer setColor(int r, int g, int b, int a) { this.ensureNotEmpty(); this.original.setColor(r, g, b, a); return this; }
		@Override public VertexConsumer setColor(int color) { this.ensureNotEmpty(); this.original.setColor(color); return this; }
		@Override public VertexConsumer setUv1(int u, int v) { this.ensureNotEmpty(); this.original.setUv1(u, v); return this; }
		@Override public VertexConsumer setUv2(int u, int v) { this.ensureNotEmpty(); this.original.setUv2(u, v); return this; }
		@Override public VertexConsumer setNormal(float x, float y, float z) { this.ensureNotEmpty(); this.original.setNormal(x, y, z); return this; }
	}
}
