package net.blockomorph.core.render.blockGetter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import oshi.annotation.concurrent.NotThreadSafe;

public abstract class ProxyPlayerBlockGetter implements InPlayerBlockAndTintGetter {
	private final ProxyLightEngine lightEngine = new ProxyLightEngine(this);
	private final InPlayerManager manager;
	private final BlockInfo[] blocksSnapshot = new BlockInfo[BlocksInPlayerStorage.SIZE];
	private final IntList activePoses = new IntArrayList(BlocksInPlayerStorage.ONE_AXIS);
	private final IntList bufferPoses = new IntArrayList(BlocksInPlayerStorage.ONE_AXIS);
	private final BlockPos.MutableBlockPos blockPosHolder = new BlockPos.MutableBlockPos();
	private boolean noExternal;
	protected BlockAndTintGetter worldAccess = EMPTY;
	private LevelChunk cachedChunk;
	private BlockPos zeroRealPos;

	public ProxyPlayerBlockGetter(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
		this.cacheRealPos();
	}

	private static class BlockInfo {
		BlockPos keyPos; BlockState blockState; Object modelData; boolean shouldRenderFluid; boolean alive; boolean external;
	}

	@NotThreadSafe
	public void rebake(IntList renderCoords, IntList bufferCoords) {
		this.noExternal = false;
		this.doClear(this.activePoses);
		this.doClear(this.bufferPoses);
		this.addBlocksTo(renderCoords, true);
		this.addBlocksTo(bufferCoords, false);
		if (this.manager.level() instanceof BlockAndTintGetter getter) {
			this.worldAccess = getter;
		} else this.worldAccess = EMPTY;
		this.cacheRealPos();
		this.cachedChunk = null;
	}

	private void doClear(IntList blocksList) {
		blocksList.forEach(pos -> {
			BlockInfo info = this.blocksSnapshot[pos];
			if (info != null) { info.modelData = null; info.alive = false; info.external = false; }
		});
		blocksList.clear();
	}

	private void addBlocksTo(IntList poses, boolean render) {
		poses.forEach(coord -> {
			BlockInPlayer2 block = this.manager.getBlocksStorage().get(coord);
			if (block != null) {
				int pos = block.getOffsetAsInt();
				this.addBlock(block, render);
				if (render) this.checkBlocksFromWorld(pos);
			}
		});
	}

	private void checkBlocksFromWorld(int pos) {
		InPlayerBlockAndTintGetter.checkCornerBlocks(pos, testPos -> {
			if (this.manager.getBlocksStorage().get(testPos) != null) return true;
			BlockInfo info = this.blocksSnapshot[testPos];
			return info != null && info.alive;
		}, newPos -> {
			BlockPos real = this.translateToReal(newPos);
			int secX = SectionPos.blockToSectionCoord(real.getX());
			int secZ = SectionPos.blockToSectionCoord(real.getZ());
			LevelChunk chunk = this.cachedChunk;
			if (chunk == null || chunk.getPos().x() != secX || chunk.getPos().z() != secZ) {
				this.cachedChunk = chunk = this.manager.level().getChunk(secX, secZ);
			}
			BlockState blockState = chunk.getBlockState(real);
			Object modelData = this.extractRenderDataFrom(chunk.getBlockEntity(real));
			BlockInfo info = this.createOrGetBlockInfo(newPos);
			this.bufferPoses.add(newPos);
			info.blockState = blockState;
			info.modelData = modelData;
			info.external = true;
			info.keyPos = this.getZeroKeyPos().offset(InPlayerBlockPos.getX(newPos), InPlayerBlockPos.getY(newPos), InPlayerBlockPos.getZ(newPos));
		});
	}

	private void addBlock(BlockInPlayer2 block, boolean render) {
		int pos = block.getOffsetAsInt();
		BlockInfo info = this.createOrGetBlockInfo(pos);
		(render ? this.activePoses : this.bufferPoses).add(pos);
		info.keyPos = block.getPos();
		info.blockState = block.getBlockState();
		info.modelData = this.extractRenderDataFrom(block.getBlockEntity());
		info.shouldRenderFluid = block.shouldDoFluidAction();
	}

	private BlockInfo createOrGetBlockInfo(int pos) {
		BlockInfo info = this.blocksSnapshot[pos];
		if (info == null) {
			this.blocksSnapshot[pos] = info = new BlockInfo();
		}
		info.alive = true;
		return info;
	}

	@Override
	public void forEachRenderBlocks(Output blockConsumer) {
		for (int i = 0; i < this.activePoses.size(); i++) {
			BlockInfo info = this.blocksSnapshot[this.activePoses.getInt(i)];
			if (info != null) {
				if (!blockConsumer.order(info.keyPos, info.blockState, info.shouldRenderFluid))
					return;
			}
		}
	}

	@Override
	public int renderSize() {
		return this.activePoses.size();
	}

	@Override
	public void noExternal(boolean yes) {
		this.noExternal = yes;
	}

	@Override
	public boolean isExternalPos(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return true;
		BlockInfo info = this.blocksSnapshot[i];
		if (info != null && info.alive) {
			return info.external;
		}
		return true;
	}

	@Override
	public BlockAndTintGetter getRealWorld() {
		return this.worldAccess;
	}

	private void cacheRealPos() {
		this.zeroRealPos = BlockPos.containing(MorphMath.getRealBlockPos(this.manager.getOwner(), new Vec3(0.5, 0, 0.5)));
	}

	@Override
	public BlockPos getZeroKeyPos() {
		return this.manager.getZeroKey();
	}

	@Override
	public BlockPos realCenterPos() {
		return this.zeroRealPos;
	}

	@Override
	public BlockPos.MutableBlockPos blockPosHolder() {
		return this.blockPosHolder;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		MorphUtils.LOGGER.warn("Request block entity bypass thread-safe copy! Zero: {}, Pos: {}",
				this.manager.getZeroKey().toShortString(), pos.toShortString(), new Throwable());
		int i = this.translateToOffset(pos);
		if (i == -1) return null;
		var block = this.manager.getBlocksStorage().get(i);
		if (block != null) return block.getBlockEntity();
		return null;
	}

	protected Object getModelDataRaw(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return null;
		BlockInfo info = this.blocksSnapshot[i];
		if (info != null && info.alive) {
			if (info.external && this.noExternal) return null;
			return info.modelData;
		}
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return Blocks.AIR.defaultBlockState();
		BlockInfo info = this.blocksSnapshot[i];
		if (info != null && info.alive) {
			if (info.external && this.noExternal) return Blocks.AIR.defaultBlockState();
			return info.blockState;
		}
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public CardinalLighting cardinalLighting() {
		return this.getRealWorld().cardinalLighting();
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return this.lightEngine;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver color) {
		return this.getRealWorld().getBlockTint(this.translateToReal(pos), color);
	}

	@Override
	public int getHeight() {
		return this.getRealWorld().getHeight();
	}

	@Override
	public int getMinY() {
		return this.getRealWorld().getMinY();
	}
}
