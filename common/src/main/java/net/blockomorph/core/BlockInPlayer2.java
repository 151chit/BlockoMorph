package net.blockomorph.core;

import net.blockomorph.core.coords.*;
import net.blockomorph.core.storage.BlocksFastStorage;
import net.blockomorph.utils.MorphedBlockProblemReporter;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class BlockInPlayer2 implements BlocksFastStorage.InPlayerBlockPosed {
	private final InPlayerBlockPos offset;
	private final BlockPos id;
	private final InPlayerManager manager;
	private BlockState blockState;
	private BlockEntity blockEntity;

	protected BlockInPlayer2(InPlayerManager mn, InPlayerBlockPos pos, BlockState state, Consumer<BlockInPlayer2> preInit, @Nullable BlockEntityType<?> type) {
		this.manager = mn;
		this.offset = pos;
		this.blockState = state;
		if (!this.offset.isValid())
			throw new IllegalArgumentException("Irregular internal block pos in playerOwner: " + pos + " Player section: " + MorphedPlayerSection.forLog(mn));
		if (state.is(Blocks.AIR))
			throw new IllegalArgumentException("Empty block for pos: " + pos + " in player section: " + MorphedPlayerSection.forLog(mn));
		this.id = pos.encodeByManager(mn);
		this.blockState = state;
		preInit.accept(this);
		this.primaryInitBlockEntity(type);
	}

	private void primaryInitBlockEntity(BlockEntityType<?> type) {
		if (type == null) {
			this.initBlockEntity();
		} else {
			BlockEntity blockEntity1 = type.create(this.id, this.blockState);
			if (blockEntity1.isValidBlockState(this.blockState)) {
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
		return id;
	}
	public long getSectionId() {
		return this.manager.getSectionId();
	}

	public final InPlayerBlockPos getOffset() {
		return offset;
	}

	@Override
	public final int getOffsetAsInt() {
		return this.getOffset().asInt();
	}

	public InPlayerManager getOwner() {
		return this.manager;
	}

	public PlayerAccessor getPlayer() {
		return this.getOwner().getOwner();
	}

	protected boolean shouldShowFluidState() {
		return Config.get().liquidsInBlocks.getValue();
	}

	public boolean shouldDoFluidAction() {
		return this.blockState.getBlock() instanceof LiquidBlock || (!this.blockState.getFluidState().isEmpty() && this.shouldShowFluidState());
	}

	@Nullable
	public List<String> loadFullTag(CompoundTag tg) {
		if (this.blockEntity != null) {
			try {
				MorphedBlockProblemReporter collector = new MorphedBlockProblemReporter(25, 200);
				ValueInput valueInput = TagValueInput.create(collector, this.manager.level().registryAccess(), tg);
				this.blockEntity.loadWithComponents(valueInput);
				return collector.getProblemsIfNotEmpty();
			} catch (Throwable e) {
				return List.of(Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()));
			}
		}
		return null;
	}

	protected void onPlace(BlockState newState, BlockState oldState, int flags) {
		if (this.manager.isServer())
			newState.onPlace(this.manager.level(), this.id, oldState, (flags & 64) != 0);
	}

	protected void changeBlockState(BlockState state, int flags) {
		BlockState old = this.blockState;
		this.blockState = state;
		boolean newBlock = !old.is(state.getBlock());
		boolean bl5 = (flags & 256) == 0;

		if (newBlock && old.hasBlockEntity() && !state.shouldChangedStateKeepBlockEntity(old)) {
			if (this.manager.isServer() && bl5) {
				if (this.blockEntity != null) {
					this.blockEntity.preRemoveSideEffects(this.id, old);
				}
			}

			this.clearBlockEntity();
		}

		if (newBlock || state.getBlock() instanceof BaseRailBlock) {
			this.checkUpdates(flags, old);
		}

		if (this.manager.isServer() && (flags & 512) == 0) {
			this.onPlace(state, old, flags);
		}

		if (state.hasBlockEntity()) {
			if (this.blockEntity == null) {
				this.initBlockEntity();
			} else {
				this.blockEntity.setBlockState(state);
			}
		}
	}

	protected void checkUpdates(int flags, BlockState old) {
		if (this.manager.level() instanceof ServerLevel serverLevel) {
			boolean bl4 = (flags & 64) != 0;
			if ((flags & 1) != 0 || bl4) {
				old.affectNeighborsAfterRemoval(serverLevel, this.id, bl4);
			}
		}
	}

	public void forceChangeBlockEntity(BlockEntity blockEntity, boolean updateTicker) {
		if (this.blockState.hasBlockEntity()) {
			if (!blockEntity.getBlockPos().equals(this.id)) return;
			BlockState blockEntityBlockstate = blockEntity.getBlockState();
			if (blockEntityBlockstate != this.blockState) {
				if (!blockEntity.getType().isValid(this.blockState)) {
					return;
				}
				blockEntity.setBlockState(this.blockState);
			}
			blockEntity.setLevel(this.manager.level());
			blockEntity.clearRemoved();
			BlockEntity old = this.blockEntity;
			this.blockEntity = blockEntity;
			if (old != null && old != this.blockEntity && !old.isRemoved()) {
				old.setRemoved();
			}
			if (updateTicker) {
				if (this.manager.level() instanceof ServerLevel && this.blockEntity instanceof GameEventListener.Provider<?> provider) {
					this.manager.getEventListenersStorage().register(provider.getListener());
				}
				this.manager.getBlockEntityTickManager().updateBlockEntityTicker(this);
			}
		}
	}

	public void clearBlockEntity() {
		if (this.blockEntity != null) {
			if (this.manager.level() instanceof ServerLevel && this.blockEntity instanceof GameEventListener.Provider<?> provider) {
				this.manager.getEventListenersStorage().unregister(provider.getListener());
			}
			if (!this.blockEntity.isRemoved())
				this.blockEntity.setRemoved();
		}
		this.blockEntity = null;
		this.manager.getBlockEntityTickManager().removeBlockEntityTicker(this.offset);
	}

	private void initBlockEntity() {
		if (this.blockState.getBlock() instanceof EntityBlock ent) {
			BlockEntity blockEntity1 = ent.newBlockEntity(this.id, this.blockState);
			if (blockEntity1 != null) {
				this.forceChangeBlockEntity(blockEntity1, true);
			}
		}
	}
}
