package net.blockomorph.utils;

import net.blockomorph.BlockomorphServer;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Consumer;

public class BlockInPlayer2 {
	private final InPlayerBlockPos offset;
	private final BlockPos pos;
	private final Player player;
	private BlockState blockState;
	private BlockEntity blockEntity;
	private BlockEntityTicker blockEntityTicker;
	//client only \/
	private CompoundTag serverTag = new CompoundTag(); //temp

	public BlockInPlayer2(PlayerAccessor pl, InPlayerBlockPos pos, BlockState state, Consumer<BlockInPlayer2> preInit) {
		this.offset = pos;
		this.pos = pos.boundedBlockPos(pl.player());
		if (this.pos == null)
			throw new IllegalArgumentException("Null BlockPos in BlockInPlayer! Player section: " + BlockPosBounds.getChunkPosForPlayer(pl.player()) +
					" Level: " + pl.player().level());
		this.player = pl.player();
		this.blockState = state;
		preInit.accept(this);
		this.initBlockEntity();
		this.initTicker();
	}

	public BlockState getBlockState() {
		return blockState;
	}

	public BlockEntity getBlockEntity() {
		return blockEntity;
	}

	public BlockPos getPos() {
		return pos;
	}

	public InPlayerBlockPos getOffset() {
		return offset;
	}

	public BlockInPlayer2 loadNBT(CompoundTag tg) {
		if (this.blockEntity != null) {
			try {
				this.blockEntity.loadWithComponents(tg, this.player.level().registryAccess());
			} catch (Exception ignored) {}
		}
		return this;
	}

	public BlockInPlayer2 handleClientTag(CompoundTag tg, ClientBoundMorphUpdatePacket pkt) {
		if (this.blockEntity != null) {
			try {
				ClientboundBlockEntityDataPacket.create(this.blockEntity, (ent, access) -> tg).handle(pkt.getListener());
			} catch (Exception ignored) {}
		}
		return this;
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
				BlockomorphServer.LOGGER.error("An error occurred while removing block in morphed player on pos: " + this.offset + " for block: " + old + " on player: " + this.player.getName().getString(), e);
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
			}
		}
		this.initTicker();
		return this;
	}

	public void clearBlockEntity() {
		this.blockEntity = null;
		this.blockEntityTicker = null;
		this.serverTag = new CompoundTag();
	}

	private void initBlockEntity() {
		if (this.blockState.getBlock() instanceof EntityBlock ent) {
			this.blockEntity = ent.newBlockEntity(this.pos, this.blockState);
			if (blockEntity != null) {
				this.blockEntity.setLevel(this.player.level());
			}
		}
	}

	private void initTicker() {
		if (this.blockState.getBlock() instanceof EntityBlock ent && blockEntity != null) {
			this.blockEntityTicker = ent.getTicker(this.player.level(), this.blockState, blockEntity.getType());
		}
	}

	public CompoundTag getServerTag() {
		return this.serverTag;
	}

	public BlockInPlayer2 setServerTag(CompoundTag serverTag) {
		if (this.blockEntity != null && this.player.level().isClientSide)
			this.serverTag = Objects.requireNonNullElse(serverTag, new CompoundTag());
		return this;
	}

	public void tick() {
		if (this.blockEntity != null) {
			if (this.blockEntityTicker != null) {
				try {
					blockEntityTicker.tick(this.player.level(), this.pos, this.blockState, this.blockEntity);
				} catch (Exception e) {
					this.blockEntityTicker = null;
					BlockomorphServer.LOGGER.error(
							"An unexpected exception occurred while ticking a block entity in a transformed player with username " +
									this.player.getName().getString() +
									": ", e
					);
				}
			}
		}
	}

	public void animateTick() {
		if (this.player.level().isClientSide) {
			this.blockState.getBlock().animateTick(this.blockState, this.player.level(), this.pos, this.player.getRandom());
		}
	}

	private boolean needRemoveBlockEntity(BlockState old, BlockState newState) {
		return old.hasBlockEntity() && (!old.is(newState.getBlock()) || !newState.hasBlockEntity());
	}
}
