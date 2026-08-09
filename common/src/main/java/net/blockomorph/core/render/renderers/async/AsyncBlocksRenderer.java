package net.blockomorph.core.render.renderers.async;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncBlocksRenderer extends BakedBlocksRenderer {
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private final Vector3f playerPosOffset = new Vector3f();
	private final EnumMap<PlayerSectionLayer, PlayerRenderLayer> renderLayers = new EnumMap<>(PlayerSectionLayer.class);
	private final AsyncRenderersStorage rootStorage;
	private final SectionPos pos;
	private VertexSorting sorter;
	private volatile AsyncState state = AsyncState.OFF;
	private volatile PlayerSectionCompileQueue.PlayerCompileTask bakeTask;
	private RebakeReason currentRebakeTask = RebakeReason.NO;
	private RebakeReason bakeAsyncCorrupted = RebakeReason.NO;
	private boolean hasOld, stopWithClean;
	private volatile boolean finallyTerminate;
	private final AtomicBoolean destroying = new AtomicBoolean();

	protected AsyncBlocksRenderer(UUID ownerId, SectionPos pos, AsyncRenderersStorage rootStorage) {
		super(ownerId);
		this.rootStorage = rootStorage;
		this.pos = pos;
		for (PlayerSectionLayer layer : PlayerSectionLayer.values())
			this.renderLayers.put(layer, new PlayerRenderLayer(layer));
	}

	protected AsyncRenderersStorage getRootStorage() {
		return this.rootStorage;
	}

	protected SectionPos getPos() {
		return this.pos;
	}

	@Override
	protected VertexConsumer getOutput(PlayerSectionLayer layer) {
		return this.renderLayers.get(layer).makeOrGetBuilder();
	}

	protected ImmediateRenderState checkAsyncAndGetWorkStatus(AsyncDispatcher renderer) {
		this.ensureActive();
		return switch (renderer.getWorkStatus(this.pos)) {
			case REBAKE -> this.rebake(renderer, RebakeReason.REBUILD);
			case RESORT -> this.rebake(renderer, RebakeReason.RESORT);
			case NO -> {
				if (!this.state.stable()) {
					this.stopWithClean = true;
					this.bakeTask.cancel();
				} else {
					this.resetState();
				}
				this.hasOld = false;
				yield ImmediateRenderState.ALLOW;
			}
			case YES -> {
				if (this.bakeAsyncCorrupted.needRebake()) {
					RebakeReason info = this.bakeAsyncCorrupted;
					this.bakeAsyncCorrupted = RebakeReason.NO;
					yield this.rebake(renderer, info);
				}
				yield this.hasOld ? ImmediateRenderState.DISABLED : ImmediateRenderState.FREEZE;
			}
		};
	}

	private void resetState() {
		this.state = AsyncState.OFF;
		this.stopWithClean = false;
		this.bakeAsyncCorrupted = RebakeReason.NO;
		this.currentRebakeTask = RebakeReason.NO;
		this.releaseUploadingWaited();
	}

	private void releaseUploadingWaited() {
		for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			this.renderLayers.get(layer).releaseTemp();
		}
	}

	private ImmediateRenderState rebake(AsyncDispatcher renderer, RebakeReason rebakeReason) {
		if (!rebakeReason.needRebake()) throw new IllegalStateException(rebakeReason.name());
		if (this.state.stable()) {
			this.state = AsyncState.BAKING;
			this.bakeTask = rebakeReason == RebakeReason.REBUILD ? this.formRebuildTask(renderer) : this.formResortTask();
			this.bakeSorter();
			this.releaseUploadingWaited();
			PlayersAsyncBakersManager.Provider.getFromVanilla().scheduleCompileTask(this.bakeTask);
		} else {
			this.stopAsyncWithTask(rebakeReason);
		}
		this.stopWithClean = false;
		return this.hasOld ? ImmediateRenderState.DISABLED : ImmediateRenderState.FREEZE;
	}

	private PlayerSectionCompileQueue.PlayerCompileTask formResortTask() {
		EnumMap<PlayerSectionLayer, MeshData.SortState> stateEnumMap = new EnumMap<>(PlayerSectionLayer.class);
		for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			var playerLayer = this.renderLayers.get(layer);
			if (playerLayer.translucentSortInfo != null) {
				stateEnumMap.put(layer, playerLayer.translucentSortInfo);
			}
		}
		this.currentRebakeTask = RebakeReason.RESORT;
		return new PlayerSectionCompileQueue.PlayerCompileTask(this, () -> {
			stateEnumMap.forEach((layer, sortState) -> this.renderLayers.get(layer).resortIndexes(sortState));
			this.onEnd(false);
		});
	}

	private PlayerSectionCompileQueue.PlayerCompileTask formRebuildTask(AsyncDispatcher renderer) {
		var blocksSnapshot = renderer.makeSnapshot(this.pos);
		this.currentRebakeTask = RebakeReason.REBUILD;
		return new PlayerSectionCompileQueue.PlayerCompileTask(this, () -> {
			try {
				this.renderBlocks(blocksSnapshot);
			} finally {
				this.onEnd(true);
			}
		});
	}

	private void bakeSorter() {
		float deltaTick = GuiUtils.MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 sectionOrigin = MorphMath.playerSectionOrigin(this.rootStorage.playerOwner(), deltaTick);
		Vec3 camPos = PlayersAsyncBakersManager.Provider.getFromVanilla().cameraPos();
		float localX = (float) (camPos.x - sectionOrigin.x);
		float localY = (float) (camPos.y - sectionOrigin.y);
		float localZ = (float) (camPos.z - sectionOrigin.z);
		this.sorter = VertexSorting.byDistance(localX, localY, localZ);
	}

	protected void draw(PlayerSectionLayerGroup group, Vec3 camPos, float deltaTick) {
		this.ensureActive();
		this.checkBuffers();
		if (this.setupMatrix(camPos, deltaTick))
			this.drawInternal(group);
	}

	private void checkBuffers() {
		if (this.state == AsyncState.UPLOADING_WAIT) {
			if (!this.stopWithClean) {
				if (this.bakeAsyncCorrupted.needRebake()) {
					this.releaseUploadingWaited();
				} else for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
					if (this.currentRebakeTask == RebakeReason.REBUILD) {
						this.renderLayers.get(layer).uploadMesh();
					} else if (this.currentRebakeTask == RebakeReason.RESORT) {
						this.renderLayers.get(layer).uploadResortBuffer();
					} else throw new IllegalStateException();
				}
				this.currentRebakeTask = RebakeReason.NO;
				this.hasOld = true;
				this.state = AsyncState.ON;
			} else {
				this.resetState();
			}
		}
	}

	private void drawInternal(PlayerSectionLayerGroup group) {
		if (this.hasOld && this.state != AsyncState.OFF) {
			RenderTarget texture = group.chunkType().outputTarget();
			GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
					.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, this.playerPosOffset, TEXTURE_MATRIX);
			try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder()
					.createRenderPass(() -> "Player section layers: " + group.name() + " for playerOwner: " + this.ownerId + " for pos: " + this.pos,
							Objects.requireNonNull(texture.getColorTextureView()), OptionalInt.empty(),
							texture.getDepthTextureView(), OptionalDouble.empty())) {
				for (PlayerSectionLayer layer : group.layers()) {
					var playerLayer = this.renderLayers.get(layer);
					if (!playerLayer.isReady()) continue;
					MeshData.DrawState drawState = playerLayer.buildResultInfo;
					renderPass.setPipeline(selectPipeline(layer));
					RenderSystem.bindDefaultUniforms(renderPass);
					renderPass.setUniform("DynamicTransforms", dynamicTransforms);
					var blockAtlas = GuiUtils.MC.getTextureManager().getTexture(Sheets.BLOCKS_MAPPER.sheet());
					renderPass.bindTexture("Sampler0", blockAtlas.getTextureView(), blockAtlas.getSampler());
					renderPass.bindTexture("Sampler2", GuiUtils.MC.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
					renderPass.setVertexBuffer(0, playerLayer.vertexBuffer.buffer);
					GpuBuffer indexBuf = playerLayer.indexBuffer.buffer;
					VertexFormat.IndexType indexType = drawState.indexType();
					if (!playerLayer.supportIndexes) {
						RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
						indexBuf = autoIndices.getBuffer(drawState.indexCount());
						indexType = autoIndices.type();
					}
					renderPass.setIndexBuffer(indexBuf, indexType);
					renderPass.drawIndexed(0, 0, drawState.indexCount(), 1);
				}
			}
		}
	}

	private boolean setupMatrix(Vec3 camPos, float deltaTick) {
		PlayerAccessor pl = this.rootStorage.playerOwner();
		if (pl == null) return false;
		MorphMath.playerPosForRender(pl, deltaTick, camPos, this.playerPosOffset::set);
		return true;
	}

	private static RenderPipeline selectPipeline(PlayerSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderPipelines.SOLID_BLOCK;
			case CUTOUT -> RenderPipelines.CUTOUT_BLOCK;
			case TRANSLUCENT -> RenderPipelines.TRANSLUCENT_BLOCK;
		};
	}

	private void onEnd(boolean needUploadMesh) {
		if (this.finallyTerminate) {
			this.destroy();
			return;
		}
		if (needUploadMesh) for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			this.renderLayers.get(layer).terminateBuilderAndTrySaveMesh();
		}
		this.bakeTask = null;
		this.state = AsyncState.UPLOADING_WAIT;
		if (this.finallyTerminate) {
			this.destroy();
		}
	}

	@Override
	protected boolean isInterrupted() {
		return this.bakeTask.isCancelled() || this.finallyTerminate || Thread.currentThread().isInterrupted();
	}

	private void ensureActive() {
		if (this.finallyTerminate)
			throw new IllegalStateException("Need delete this instance!");
	}

	private void stopAsyncWithTask(RebakeReason rebakeReason) {
		this.bakeTask.cancel();
		this.bakeAsyncCorrupted = rebakeReason.compareAndUp(this.bakeAsyncCorrupted);
	}

	protected void destroyAll() {
		this.ensureActive();
		this.finallyTerminate = true;
		if (this.state.stable()) {
			this.destroy();
		}
		for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			this.renderLayers.get(layer).vertexBuffer.close();
			this.renderLayers.get(layer).indexBuffer.close();
		}
	}

	private void destroy() {
		if (this.destroying.compareAndSet(false, true)) {
			this.releaseUploadingWaited();
		}
	}

	enum AsyncState {
		OFF, BAKING, UPLOADING_WAIT, ON;

		boolean stable() {
			return this != BAKING;
		}
	}

	enum RebakeReason {
		NO, RESORT, REBUILD;

		boolean needRebake() {
			return this != NO;
		}

		RebakeReason compareAndUp(RebakeReason oldReason) {
			if (oldReason == null) return this;
			if (this.ordinal() >= oldReason.ordinal()) return this;
			return oldReason;
		}
	}

	public enum ImmediateRenderState {
		ALLOW, FREEZE, DISABLED;

		public boolean enabled() {
			return this != DISABLED;
		}

		public boolean needSnapshot() {
			return this == ALLOW;
		}
	}

	class PlayerRenderLayer {
		final PlayerSectionLayer layer;

		final ByteBufferBuilder allocator;
		BufferBuilder currentBuilder;
		volatile MeshData tempBuilderResult;
		volatile MeshData.SortState tempSortState;
		volatile ByteBufferBuilder.Result tempResortInfo;

		final AutoResizeGpuBuffer vertexBuffer;
		final AutoResizeGpuBuffer indexBuffer; boolean supportIndexes;
		MeshData.DrawState buildResultInfo;
		MeshData.SortState translucentSortInfo;

		PlayerRenderLayer(PlayerSectionLayer layer) {
			this.layer = layer;
			this.allocator = new ByteBufferBuilder(layer.baseBufferSize());
			this.vertexBuffer = new AutoResizeGpuBuffer(GpuBuffer.USAGE_VERTEX, layer);
			this.indexBuffer = new AutoResizeGpuBuffer(GpuBuffer.USAGE_INDEX, layer);
		}

		BufferBuilder makeOrGetBuilder() {
			var meshBuilder = this.currentBuilder;
			if (meshBuilder == null) {
				this.currentBuilder = meshBuilder = new BufferBuilder(this.allocator, VertexFormat.Mode.QUADS, this.layer.chunkType().vertexFormat());
			}
			return meshBuilder;
		}

		void terminateBuilderAndTrySaveMesh() {
			if (this.currentBuilder != null) {
				this.tempBuilderResult = this.currentBuilder.buildOrThrow();
				if (this.layer.isTranslucent()) {
					this.tempSortState = this.tempBuilderResult.sortQuads(this.allocator, sorter);
				} else this.tempSortState = null;
				this.currentBuilder = null;
			}
		}

		void resortIndexes(MeshData.SortState oldState) {
			this.tempResortInfo = oldState.buildSortedIndexBuffer(this.allocator, sorter);
		}

		void releaseTemp() {
			if (this.tempResortInfo != null) {
				this.tempResortInfo.close();
				this.tempResortInfo = null;
			}
			if (this.tempBuilderResult != null) {
				this.tempBuilderResult.close();
				this.tempBuilderResult = null;
			}
			this.tempSortState = null;
			this.allocator.discard();
		}

		void uploadResortBuffer() {
			if (this.tempResortInfo != null) {
				this.indexBuffer.uploadMeshDataPart(this.tempResortInfo.byteBuffer());
				this.releaseTemp();
			}
		}

		void uploadMesh() {
			if (this.tempBuilderResult != null) {
				this.translucentSortInfo = this.tempSortState;
				this.vertexBuffer.uploadMeshDataPart(this.tempBuilderResult.vertexBuffer());
				var index = this.tempBuilderResult.indexBuffer();
				this.supportIndexes = index != null;
				if (this.supportIndexes)
					this.indexBuffer.uploadMeshDataPart(index);
				this.buildResultInfo = this.tempBuilderResult.drawState();
			} else {
				this.buildResultInfo = null;
				this.translucentSortInfo = null;
			}
			this.releaseTemp();
		}

		boolean isReady() {
			return this.buildResultInfo != null;
		}
	}

	class AutoResizeGpuBuffer {
		final PlayerSectionLayer layer;
		final int usage;
		GpuBuffer buffer;

		AutoResizeGpuBuffer(int usage, PlayerSectionLayer layer) {
			this.usage = usage;
			this.layer = layer;
			this.createBuffer(this.layer.baseBufferSize());
		}

		void createBuffer(long capacity) {
			if (this.buffer != null) this.buffer.close();
			this.buffer = RenderSystem.getDevice().createBuffer(
					() -> "Player render layer. Player id: " + ownerId + " Layer: " + this.layer + " Usage: " + this.usage + " Pos: " + pos,
					this.usage | GpuBuffer.USAGE_COPY_DST, capacity);
		}

		void uploadMeshDataPart(ByteBuffer buffer) {
			if (this.buffer.size() <= buffer.remaining()) {
				PlayerAccessor pl = rootStorage.playerOwner();
				MorphUtils.LOGGER.info("Resizing player's section VBO for {} Layer: {} For size: {} For section: {}",
						pl != null ? pl : ownerId, this.layer, buffer.remaining(), pos.toShortString());
				this.createBuffer(buffer.remaining() + 32L);
			}
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0, buffer.remaining()), buffer);
		}

		void close() {
			this.buffer.close();
		}
	}
}