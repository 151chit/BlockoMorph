package net.blockomorph.core.render.renderers.async;

import com.google.common.primitives.Floats;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.core.render.renderers.BakedBlocksRenderer;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncBlocksRenderer extends BakedBlocksRenderer {
	private final Vector3f playerPosOffset = new Vector3f();
	private final EnumMap<PlayerSectionLayer, PlayerRenderLayer> renderLayers = new EnumMap<>(PlayerSectionLayer.class);
	private final List<PlayerSectionMeshBuffer> buffersForRender = new ObjectArrayList<>(PlayerSectionLayer.values().length);
	private final Set<TextureAtlasSprite> animatedSprites = new ObjectOpenHashSet<>(BlocksInPlayerStorage.ONE_AXIS);
	private final AsyncRenderersStorage rootStorage;
	private final SectionPos pos;
	private VertexSorting sorter;
	private volatile AsyncState state = AsyncState.OFF;
	private PlayerSectionCompileQueue.PlayerCompileTask bakeTask;
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
					yield this.rebake(renderer, this.bakeAsyncCorrupted);
				}
				yield this.hasOld ? ImmediateRenderState.DISABLED : ImmediateRenderState.FREEZE;
			}
		};
	}

	private void resetState() {
		this.state = AsyncState.OFF;
		this.bakeTask = null;
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
		rebakeReason = rebakeReason.compareAndUp(this.currentRebakeTask);
		if (this.state.stable()) {
			this.state = AsyncState.BAKING;
			this.bakeAsyncCorrupted = RebakeReason.NO;
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
		this.currentRebakeTask = RebakeReason.RESORT;
		return new PlayerSectionCompileQueue.PlayerCompileTask(this, () -> {
			for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
				this.renderLayers.get(layer).resortIndexes();
			}
			this.onEnd(false);
		});
	}

	private PlayerSectionCompileQueue.PlayerCompileTask formRebuildTask(AsyncDispatcher renderer) {
		var blocksSnapshot = renderer.makeSnapshot(this.pos);
		this.currentRebakeTask = RebakeReason.REBUILD;
		this.initOnMainThread();
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
		var vec = new Vector3f(localX, localY, localZ);//todo remove
		this.sorter = (compactVectorArray) -> {//on 1.21.10 sodium sorting algorithm may be broken
			Vector3f vector3f = new Vector3f();
			float[] fs = new float[compactVectorArray.size()];
			int[] is = new int[compactVectorArray.size()];

			for(int i = 0; i < compactVectorArray.size(); is[i] = i++) {
				fs[i] = compactVectorArray.get(i, vector3f).distanceSquared(vec);
			}

			IntArrays.mergeSort(is, (ix, j) -> Floats.compare(fs[j], fs[ix]));
			return is;
		};
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
				} else {
					for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
						if (this.currentRebakeTask == RebakeReason.REBUILD) {
							this.renderLayers.get(layer).uploadMesh();
						} else if (this.currentRebakeTask == RebakeReason.RESORT) {
							this.renderLayers.get(layer).uploadResortBuffer();
						} else throw new IllegalStateException();
					}
					this.animatedSprites.clear();
					this.animatedSprites.addAll(this.getCollectedAnimatedSprites());
					this.currentRebakeTask = RebakeReason.NO;
					this.hasOld = true;
				}
				this.state = AsyncState.ON;
				this.bakeTask = null;
			} else {
				this.resetState();
			}
		}
	}

	private void drawInternal(PlayerSectionLayerGroup group) {
		if (this.hasOld && this.state != AsyncState.OFF) {
			this.animatedSprites.forEach(this::activateSprite);
			for (PlayerSectionLayer layer : group.layers()) {
				var playerLayer = this.renderLayers.get(layer);
				this.buffersForRender.add(playerLayer.meshBuffer);
			}
			PlayerSectionMeshBuffer.drawOnGpu(this.buffersForRender, group.getOutputTarget(), this.playerPosOffset, () ->
					"Player section layers: " + group.name() + " for playerOwner: " + this.ownerId + " for pos: " + this.pos);
			this.buffersForRender.clear();
		}
	}

	private boolean setupMatrix(Vec3 camPos, float deltaTick) {
		PlayerAccessor pl = this.rootStorage.playerOwner();
		if (pl == null) return false;
		MorphMath.playerPosForRender(pl, deltaTick, camPos, this.playerPosOffset::set);
		return true;
	}

	private void onEnd(boolean needUploadMesh) {
		if (this.finallyTerminate) {
			this.destroy();
			return;
		}
		if (needUploadMesh) for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			this.renderLayers.get(layer).saveMesh();
		}
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
			this.renderLayers.get(layer).meshBuffer.close();
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
		volatile MeshData tempMeshData;
		volatile ByteBufferBuilder.Result tempResortBuffer;
		volatile MeshData.SortState tempPrimarySortState;

		final PlayerSectionMeshBuffer meshBuffer;
		MeshData.SortState sortState;

		PlayerRenderLayer(PlayerSectionLayer layer) {
			this.layer = layer;
			this.allocator = new ByteBufferBuilder(layer.baseBufferSize());
			this.meshBuffer = new PlayerSectionMeshBuffer(layer, usage ->
					"Player render layer. Player id: " + ownerId + " Usage: " + usage + " Layer: " + this.layer + " Pos: " + pos,
					size -> {
						PlayerAccessor pl = rootStorage.playerOwner();
						return "Resizing player's section VBO for " + (pl != null ? pl : ownerId) + " Layer: " +
								this.layer + " For size: " + size + " For section: " + pos.toShortString();
			});
		}

		BufferBuilder makeOrGetBuilder() {
			var meshBuilder = this.currentBuilder;
			if (meshBuilder == null) {
				this.currentBuilder = meshBuilder = new BufferBuilder(this.allocator, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			}
			return meshBuilder;
		}

		void saveMesh() {
			if (this.currentBuilder != null) {
				this.tempMeshData = this.currentBuilder.build();
				if (this.tempMeshData != null && this.layer.isTranslucent()) {
					this.tempPrimarySortState = this.tempMeshData.sortQuads(this.allocator, sorter);
				} else this.tempPrimarySortState = null;
				this.currentBuilder = null;
			}
		}

		void resortIndexes() {
			if (this.sortState == null) return;
			this.tempResortBuffer = this.sortState.buildSortedIndexBuffer(this.allocator, sorter);
		}

		//main thread   \/
		void uploadResortBuffer() {
			if (this.tempResortBuffer != null) {
				this.meshBuffer.loadIndexes(this.tempResortBuffer);
			}
			this.releaseTemp();
		}

		void uploadMesh() {
			if (this.meshBuffer.loadMeshData(this.tempMeshData)) {
				this.sortState = this.tempPrimarySortState;
			} else {
				this.sortState = null;
			}
			this.releaseTemp();
		}

		void releaseTemp() {
			if (this.tempMeshData != null) {
				this.tempMeshData.close();
				this.tempMeshData = null;
			}
			if (this.tempResortBuffer != null) {
				this.tempResortBuffer.close();
				this.tempResortBuffer = null;
			}
			this.tempPrimarySortState = null;
			this.allocator.discard();
		}
	}
}
