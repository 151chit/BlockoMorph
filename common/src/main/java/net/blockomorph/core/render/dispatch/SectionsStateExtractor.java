package net.blockomorph.core.render.dispatch;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.render.RenderingPlatformService;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.blockomorph.core.render.blockGetter.ProxyPlayerBlockGetter;
import net.blockomorph.core.render.renderers.async.AsyncBlocksRenderer;
import net.blockomorph.core.render.renderers.async.AsyncDispatcher;
import net.blockomorph.core.render.renderers.async.AsyncRenderersStorage;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SectionsStateExtractor implements AsyncDispatcher {
	private final ClientInPlayerManager manager;
	private final RenderSectionInfo[] sectionInfos = new RenderSectionInfo[8];
	public static final List<SectionPos> POSES;
	private final Supplier<AsyncRenderersStorage> playerBaker;
	private final int heavyMode;

	protected SectionsStateExtractor(ClientInPlayerManager manager, int heavyMode, Supplier<AsyncRenderersStorage> playerBaker) {
		this.manager = manager.assertOnInit();
		this.playerBaker = playerBaker;
		POSES.forEach(pos -> this.sectionInfos[MorphMath.offsetSectionIndex(pos)] = new RenderSectionInfo(pos));
		this.heavyMode = heavyMode;
	}

	@Override
	public AsyncWorkNeed getWorkStatus(SectionPos pos) {
		if (this.manager.getTntHandler().isActive()) return AsyncWorkNeed.NO;
		boolean needAsync = this.manager.getBlocksStorage().size() > this.heavyMode;
		var dirtyMarker = this.manager.getSectionsDirtyMarker();
		if (needAsync) {
			boolean isDirty = dirtyMarker.isSectionDirty(pos);
			dirtyMarker.markSectionDirty(pos, false);
			var sectionInfo = this.getSection(pos);
			if (isDirty) {
				sectionInfo.flushCam();
				return AsyncWorkNeed.REBAKE;
			}
			if (sectionInfo.needResort()) return AsyncWorkNeed.RESORT;
			return AsyncWorkNeed.YES;
		}
		dirtyMarker.markSectionDirty(pos, false);
		return AsyncWorkNeed.NO;
	}

	private RenderSectionInfo getSection(SectionPos pos) {
		var section = this.getSection(pos.x(), pos.y(), pos.z());
		if (section == null) throw new IllegalArgumentException(pos.toShortString());
		return section;
	}

	private RenderSectionInfo getSection(int setionX, int sectionY, int sectionZ) {
		int index = MorphMath.offsetSectionIndex(setionX, sectionY, sectionZ);
		if (index == -1) return null;
		return this.sectionInfos[index];
	}

	@Override
	public InPlayerBlockAndTintGetter makeSnapshot(SectionPos pos) {
		return this.getSection(pos).makeSnapshot();
	}

	protected void forEachBlockInPlayer(MorphedPlayerRenderState.BlockMorph morph, Consumer<BlockInPlayer2> additionalDataExtraction) {
		this.playerBaker.get().appendPlayer(this.manager.getOwner());
		for (RenderSectionInfo info : this.sectionInfos)
			info.clearPoses();
		this.manager.getBlocksStorage().forEach(block -> {
			var posIn = block.getOffset();
			int sectionX = SectionPos.blockToSectionCoord(posIn.x);
			int sectionY = SectionPos.blockToSectionCoord(posIn.y);
			int sectionZ = SectionPos.blockToSectionCoord(posIn.z);
			this.sectionInfos[MorphMath.offsetSectionIndex(sectionX, sectionY, sectionZ)].renderBlocks.add(block.getOffsetAsInt());
			this.checkAndAddToBuffers(block);
			additionalDataExtraction.accept(block);
		});
		BlocksRenderState immediateRenderState = null;
		for (RenderSectionInfo info : this.sectionInfos) {
			var immediateState = info.checkAsyncState(this.playerBaker.get());
			if (immediateState == null) throw new IllegalArgumentException("Broken pos: " + info.pos);
			if (morph != null && immediateState.enabled()) {
				if (immediateRenderState == null) {
					immediateRenderState = RenderingPlatformService.INSTANCE.crateBlocksSnapshotRenderState(this.manager, this.heavyMode);
				}
				switch (immediateState) {
					case ALLOW -> immediateRenderState.append(info.renderBlocks, info.bufferBlocks);
					case FREEZE -> immediateRenderState.append(info.oldBlocks, info.oldBufferBlocks);
				}
			}
			if (immediateState.needSnapshot()) info.snapshotCoords();
		}
		if (morph != null) morph.immediateBlocksData = immediateRenderState;
	}

	private void checkAndAddToBuffers(BlockInPlayer2 block) {
		var posIn = block.getOffset();
		int dx = this.getAxisOffset(Direction.Axis.X, posIn);
		int dy = this.getAxisOffset(Direction.Axis.Y, posIn);
		int dz = this.getAxisOffset(Direction.Axis.Z, posIn);
		if (dx == 0 && dy == 0 && dz == 0) return;
		int ownSecX = SectionPos.blockToSectionCoord(posIn.x);
		int ownSecY = SectionPos.blockToSectionCoord(posIn.y);
		int ownSecZ = SectionPos.blockToSectionCoord(posIn.z);
		int blockPacked = posIn.asInt();
		if (dx != 0) this.addToSection(ownSecX + dx, ownSecY, ownSecZ, blockPacked);
		if (dy != 0) this.addToSection(ownSecX, ownSecY + dy, ownSecZ, blockPacked);
		if (dz != 0) this.addToSection(ownSecX, ownSecY, ownSecZ + dz, blockPacked);
		if (dx != 0 && dz != 0) this.addToSection(ownSecX + dx, ownSecY, ownSecZ + dz, blockPacked);
		if (dx != 0 && dy != 0) this.addToSection(ownSecX + dx, ownSecY + dy, ownSecZ, blockPacked);
		if (dz != 0 && dy != 0) this.addToSection(ownSecX, ownSecY + dy, ownSecZ + dz, blockPacked);
		if (dx != 0 && dy != 0 && dz != 0) this.addToSection(ownSecX + dx, ownSecY + dy, ownSecZ + dz, blockPacked);
	}

	private int getAxisOffset(Direction.Axis axis, InPlayerBlockPos pos) {
		int coord = SectionPos.sectionRelative(pos.get(axis));
		if (coord >= 14) return 1;
		if (coord <= 1) return -1;
		return 0;
	}

	private void addToSection(int secX, int secY, int secZ, int blockPacked) {
		int index = MorphMath.offsetSectionIndex(secX, secY, secZ);
		if (index != -1) {
			this.sectionInfos[index].bufferBlocks.add(blockPacked);
		}
	}

	private class RenderSectionInfo {
		final SectionPos pos;
		final ProxyPlayerBlockGetter asyncPart = RenderingPlatformService.INSTANCE.createBlocksSnapshotHolderForAsync(manager);
		final IntList renderBlocks = new IntArrayList();
		final IntList bufferBlocks = new IntArrayList();
		final IntList oldBlocks = new IntArrayList();
		final IntList oldBufferBlocks = new IntArrayList();
		Vec3 lastCamPos;

		RenderSectionInfo(SectionPos pos) {
			this.pos = pos;
		}

		AsyncBlocksRenderer.ImmediateRenderState checkAsyncState(AsyncRenderersStorage storage) {
			return storage.checkSection(SectionsStateExtractor.this, this.pos);
		}

		boolean needResort() {
			Vec3 camPos = PlayersAsyncBakersManager.Provider.getFromVanilla().cameraPos();
			if (this.lastCamPos == null || this.lastCamPos.distanceToSqr(camPos) > 1) {
				this.lastCamPos = camPos;
				return true;
			}
			return false;
		}

		void flushCam() {
			this.lastCamPos = PlayersAsyncBakersManager.Provider.getFromVanilla().cameraPos();
		}

		void snapshotCoords() {
			this.oldBlocks.clear();
			this.oldBufferBlocks.clear();
			this.oldBlocks.addAll(this.renderBlocks);
			this.oldBufferBlocks.addAll(this.bufferBlocks);
		}

		void clearPoses() {
			this.renderBlocks.clear();
			this.bufferBlocks.clear();
		}

		InPlayerBlockAndTintGetter makeSnapshot() {
			this.asyncPart.rebake(this.renderBlocks, this.bufferBlocks);
			return this.asyncPart;
		}
	}

	static {
		ImmutableList.Builder<SectionPos> builder = ImmutableList.builderWithExpectedSize(8);
		for (int x = -1; x <= 0; x++) {
			for (int z = -1; z <= 0; z++) {
				for (int y = 0; y <= 1; y++) {
					builder.add(SectionPos.of(x, y, z));
				}
			}
		}
		POSES = builder.build();
	}
}
