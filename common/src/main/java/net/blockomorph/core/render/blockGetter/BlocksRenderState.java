package net.blockomorph.core.render.blockGetter;

import it.unimi.dsi.fastutil.ints.*;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.utils.side.MinecraftThreadLocal;
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

import java.util.function.Consumer;

public abstract class BlocksRenderState implements InPlayerBlockAndTintGetter {
	private final ProxyLightEngine lightEngine = new ProxyLightEngine(this);
	private final MinecraftThreadLocal<BlockPos.MutableBlockPos> blockPosHolder =
			new MinecraftThreadLocal<>(true, BlockPos.MutableBlockPos::new);
	private final MinecraftThreadLocal<LevelChunk> cachedChunk = new MinecraftThreadLocal<>(true, null);
	private final MinecraftThreadLocal<Boolean> noExternal = new MinecraftThreadLocal<>(true, () -> Boolean.FALSE);
	private final Int2ObjectMap<BlockInfo> renderBlocks;
	private final InPlayerManager manager;
	private final BlockAndTintGetter asyncWorldAccess;
	private final int capacity;
	private BlockPos zeroRealPos;
	private int size;

	protected BlocksRenderState(InPlayerManager manager, int maxCapacity) {
		this.manager = manager;
		this.renderBlocks = new Int2ObjectOpenHashMap<>((int) (maxCapacity * 2.6));
		this.asyncWorldAccess = manager.level() instanceof BlockAndTintGetter getter ? getter : EMPTY;
		this.cacheRealPos();
		this.capacity = maxCapacity;
	}

	public void append(IntList renderBlocks, IntList bufferBlocks) {
		if (renderBlocks.size() > this.capacity)
			throw new IllegalArgumentException(renderBlocks.size() + "");
		renderBlocks.forEach(pos -> {
			BlockInfo info = this.renderBlocks.get(pos);
			if (info == null) {
				this.fillAndPutBlock(pos, block -> block.includeInRender = true);
				this.checkBlocksFromWorld(pos);
				this.size++;
			} else if (!info.includeInRender) {
				info.includeInRender = true;
				this.checkBlocksFromWorld(pos);
				this.size++;
			}
		});
		bufferBlocks.forEach(pos -> {
			BlockInfo info = this.renderBlocks.get(pos);
			if (info == null) {
				this.fillAndPutBlock(pos, null);
			}
		});
		this.cacheRealPos();
	}

	private void checkBlocksFromWorld(int pos) {
		InPlayerBlockAndTintGetter.checkCornerBlocks(pos, testPos -> {
			if (this.manager.getBlocksStorage().get(testPos) != null) return true;
			return this.renderBlocks.containsKey(testPos);
		}, newPos -> {
			BlockPos real = this.translateToReal(newPos);
			int secX = SectionPos.blockToSectionCoord(real.getX());
			int secZ = SectionPos.blockToSectionCoord(real.getZ());
			LevelChunk chunk = this.cachedChunk.get();
			if (chunk == null || chunk.getPos().x() != secX || chunk.getPos().z() != secZ) {
				chunk = this.manager.level().getChunk(secX, secZ);
				this.cachedChunk.set(chunk);
			}
			BlockState blockState = chunk.getBlockState(real);
			Object modelData = this.extractRenderDataFrom(chunk.getBlockEntity(real));
			BlockInfo info = new BlockInfo();
			info.blockState = blockState;
			info.modelData = modelData;
			info.external = true;
			info.keyPos = this.getZeroKeyPos().offset(InPlayerBlockPos.getX(newPos), InPlayerBlockPos.getY(newPos), InPlayerBlockPos.getZ(newPos));
			this.renderBlocks.put(newPos, info);
		});
	}

	private void fillAndPutBlock(int pos, Consumer<BlockInfo> additional) {
		BlockInPlayer2 block = this.manager.getBlocksStorage().get(pos);
		if (block != null) {
			BlockInfo info = new BlockInfo();
			info.keyPos = block.getPos();
			info.blockState = block.getBlockState();
			info.modelData = this.extractRenderDataFrom(block.getBlockEntity());
			info.shouldRenderFluid = block.shouldDoFluidAction();
			if (additional != null) additional.accept(info);
			this.renderBlocks.put(pos, info);
		}
	}

	private static class BlockInfo {
		BlockPos keyPos; BlockState blockState; Object modelData; boolean shouldRenderFluid; boolean includeInRender; boolean external;
	}

	@Override
	public void forEachRenderBlocks(Output blockConsumer) {
		for (BlockInfo info : this.renderBlocks.values()) {
			if (info.includeInRender) {
				if (!blockConsumer.order(info.keyPos, info.blockState, info.shouldRenderFluid))
					return;
			}
		}
	}

	@Override
	public int renderSize() {
		return this.size;
	}

	@Override
	public void noExternal(boolean yes) {
		this.noExternal.set(yes);
	}

	@Override
	public boolean isExternalPos(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return true;
		BlockInfo data = this.renderBlocks.get(i);
		if (data == null) return true;
		return data.external;
	}

	@Override
	public BlockAndTintGetter getRealWorld() {
		return this.asyncWorldAccess;
	}

	@Override
	public BlockPos getZeroKeyPos() {
		return this.manager.getZeroKey();
	}

	private void cacheRealPos() {
		this.zeroRealPos = BlockPos.containing(MorphMath.getRealBlockPos(this.manager.getOwner(), new Vec3(0.5, 0, 0.5)));
	}

	@Override
	public BlockPos realCenterPos() {
		return this.zeroRealPos;
	}

	@Override
	public BlockPos.MutableBlockPos blockPosHolder() {
		return this.blockPosHolder.get();
	}

	@Override
	public CardinalLighting cardinalLighting() {
		return this.asyncWorldAccess.cardinalLighting();
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver color) {
		return this.getRealWorld().getBlockTint(this.translateToReal(pos), color);
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return this.lightEngine;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return null;
		var block = this.manager.getBlocksStorage().get(i);
		if (block == null) return null;
		return block.getBlockEntity();
	}

	protected Object getModelDataRaw(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return null;
		BlockInfo data = this.renderBlocks.get(i);
		if (data == null || (data.external && this.noExternal.get())) return null;
		return data.modelData;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		int i = this.translateToOffset(pos);
		if (i == -1) return Blocks.AIR.defaultBlockState();
		BlockInfo data = this.renderBlocks.get(i);
		if (data == null || (data.external && this.noExternal.get())) return Blocks.AIR.defaultBlockState();
		return data.blockState;
	}

	@Override
	public int getHeight() {
		return this.asyncWorldAccess.getHeight();
	}

	@Override
	public int getMinY() {
		return this.asyncWorldAccess.getMinY();
	}
}
