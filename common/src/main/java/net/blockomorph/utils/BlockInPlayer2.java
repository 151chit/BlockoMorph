package net.blockomorph.utils;

import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BlockInPlayer2 {
	private final InPlayerBlockPos offset;
	private final BlockPos pos;
	private final Player player;
	private final PlayerAccessor owner;
	private BlockState blockState;
	private BlockEntity blockEntity;
	//client only \/
	private Object additionalRenderData = null;

	public BlockInPlayer2(PlayerAccessor pl, InPlayerBlockPos pos, BlockState state, Consumer<BlockInPlayer2> preInit, @Nullable BlockEntityType<?> type) {
		this.offset = pos;
		this.pos = pos.boundedBlockPos(pl.player());
		if (this.pos == null)
			throw new IllegalArgumentException("Null BlockPos in BlockInPlayer! Player section: " + BlockPosBounds.getChunkPosForPlayer(pl.player()) +
					" Level: " + pl.player().level());
		this.player = pl.player();
		this.owner = pl;
		this.blockState = state;
		preInit.accept(this);
		this.primaryInitBlockEntity(type);
	}

	private void primaryInitBlockEntity(BlockEntityType<?> type) {
		if (type == null) {
			this.initBlockEntity();
		} else {
			BlockEntity blockEntity1 = type.create(this.pos, this.blockState);
			if (blockEntity1 != null && blockEntity1.isValidBlockState(this.blockState)) {
				this.forceChangeBlockEntity(blockEntity1, true);
			} else this.initBlockEntity();
		}
	}

	public BlockState getBlockState() {
		return blockState;
	}

	public BlockEntity getBlockEntity() {
		if (this.blockEntity != null && this.blockEntity.isRemoved()) {
			this.clearBlockEntity();
			return null;
		}
		return blockEntity;
	}

	public BlockPos getPos() {
		return pos;
	}

	public InPlayerBlockPos getOffset() {
		return offset;
	}

	public PlayerAccessor getPlayer() {
		return this.owner;
	}

	protected boolean shouldShowFluidState() {
		return Config.get().liquidsInBlocks.getValue();
	}

	public boolean shouldDoFluidAction() {
		return this.blockState.getBlock() instanceof LiquidBlock || (!this.blockState.getFluidState().isEmpty() && this.shouldShowFluidState());
	}

	@Nullable
	public Throwable loadNBT(CompoundTag tg) {
		if (this.blockEntity != null) {
			try {
				this.blockEntity.loadWithComponents(tg, this.player.level().registryAccess());
			} catch (Throwable e) {
				return e;
			}
		}
		return null;
	}

	public void handleClientTag(CompoundTag tg, ClientBoundMorphUpdatePacket pkt) {
		if (this.blockEntity != null) {
			try {
				CommonPlatformUtils.INSTANCE.loadNbtToBlockEntityOnClient(this.blockEntity, pkt, this.player.registryAccess(), tg);
			} catch (Exception ignored) {
			}
		}
	}

	public void onPlace(BlockState newState, BlockState oldState, boolean update) {
		if (!this.player.level().isClientSide)
			newState.onPlace(this.player.level(), this.pos, oldState, update);
	}

	public BlockInPlayer2 changeBlockState(BlockState state, boolean update) {
		BlockState old = this.blockState;
		this.blockState = state;
		if (!this.player.level().isClientSide) {
			try {
				old.onRemove(this.player.level(), this.pos, state, update);
			} catch (Exception e) {
				MorphUtils.LOGGER.error("An error occurred while removing block in morphed player on pos: " + this.offset + " for block: " + old + " on player: " + this.player.getName().getString(), e);
			} finally {
				if (this.needRemoveBlockEntity(old, state))
					this.clearBlockEntity();
			}
		} else if (this.needRemoveBlockEntity(old, state)) {
			this.clearBlockEntity();
		}
		this.onPlace(state, old, update);
		if (state.hasBlockEntity()) {
			if (this.blockEntity == null) {
				this.initBlockEntity();
			} else {
				this.blockEntity.setBlockState(state);
				this.owner.getBlockEntityTickManager().updateBlockEntityTicker(this);
			}
		}
		return this;
	}

	public void forceChangeBlockEntity(BlockEntity blockEntity, boolean updateTicker) {
		if (this.blockState.hasBlockEntity()) {
			if (!blockEntity.getBlockPos().equals(this.pos)) return;
			BlockState blockEntityBlockstate = blockEntity.getBlockState();
			if (blockEntityBlockstate != this.blockState) {
				if (!blockEntity.getType().isValid(this.blockState)) {
					return;
				}
				blockEntity.setBlockState(this.blockState);
			}
			blockEntity.setLevel(this.player.level());
			blockEntity.clearRemoved();
			BlockEntity old = this.blockEntity;
			this.blockEntity = blockEntity;
			if (old != null && old != this.blockEntity && !old.isRemoved()) {
				old.setRemoved();
			}
			if (updateTicker) {
				if (this.player.level() instanceof ServerLevel && this.blockEntity instanceof GameEventListener.Provider<?> provider) {
					this.owner.getListenersStorage().add(provider.getListener());
				}
				this.owner.getBlockEntityTickManager().updateBlockEntityTicker(this);
			}
		}
	}

	public void clearBlockEntity() {
		if (this.blockEntity != null) {
			if (this.player.level() instanceof ServerLevel && this.blockEntity instanceof GameEventListener.Provider<?> provider) {
				this.owner.getListenersStorage().remove(provider.getListener());
			}
			if (!this.blockEntity.isRemoved())
				this.blockEntity.setRemoved();
		}
		this.blockEntity = null;
		this.owner.getBlockEntityTickManager().removeBlockEntityTicker(this.offset);
	}

	private void initBlockEntity() {
		if (this.blockState.getBlock() instanceof EntityBlock ent) {
			BlockEntity blockEntity1 = ent.newBlockEntity(this.pos, this.blockState);
			if (blockEntity1 != null) {
				this.forceChangeBlockEntity(blockEntity1, true);
			}
		}
	}

	public void connectAdditionalRenderData(Object data) {
		this.additionalRenderData = data;
	}

	public Object getModelData() {
		return this.additionalRenderData;
	}

	public void animateTick(RandomSource randomSource, Block marker, Consumer<BlockState> needSpawnFluidDrip) {
		if (this.player.level().isClientSide) {
			this.blockState.getBlock().animateTick(this.blockState, this.player.level(), this.pos, randomSource);
		}
		if (this.shouldDoFluidAction()) {
			FluidState fluidState = this.blockState.getFluidState();
			if (!fluidState.isEmpty()) {
				fluidState.animateTick(this.player.level(), this.pos, randomSource);
				needSpawnFluidDrip.accept(this.owner.getBlockState(this.offset.offset(0, -1, 0)));
			}
		}
		if (this.blockState.getBlock() == marker) {
			Vec3 real = MorphMath.getCenteredRealBlockPos(this.owner, this.offset);
			this.player.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, this.blockState), real.x, real.y, real.z, 0.0D, 0.0D, 0.0D);
		}
	}

	private boolean needRemoveBlockEntity(BlockState old, BlockState newState) {
		return old.hasBlockEntity() && (!old.is(newState.getBlock()) || !newState.hasBlockEntity());
	}
}
