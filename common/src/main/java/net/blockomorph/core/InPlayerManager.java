package net.blockomorph.core;

import it.unimi.dsi.fastutil.ints.*;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.core.serialization.PlayerLoader;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.core.serialization.io.BlockEntityAndEntityIO;
import net.blockomorph.core.storage.BlocksInPlayerCursor3D;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.core.storage.InPlayerGameEventListenersStorage;
import net.blockomorph.core.tick.blockEntity.InPlayerBlockEntityTickManager;
import net.blockomorph.core.tick.PlayerTicks;
import net.blockomorph.network.ClientBoundApplyBlockMorphPacket;
import net.blockomorph.network.ClientBoundServerBlockEntityTagPacket;
import net.blockomorph.utils.*;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.storage.playerSection.PlayerSectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class InPlayerManager implements PlayerBlocksEditor {
	public static final int ZERO_POS = InPlayerBlockPos.ZERO_INT;
	public static final int HEAVY_MODE = 27;
	private final IntArrayList blocksForErase = new IntArrayList(BlocksInPlayerStorage.ONE_AXIS);
	private final BlockEntityAndEntityIO guiTagLoader = new BlockEntityAndEntityIO(25, 200);
	private final PlayerTicks<Block> blockTicks;
	private final PlayerTicks<Fluid> fluidTicks;
	private final HitBoxCalculator hitBoxCalculator;
	private final BlocksInPlayerStorage storage;
	private final BlocksInPlayerCursor3D cursor3D;
	private final PlayerSectionHandler sectionHandler;
	private final InPlayerBlockEntityTickManager blockEntityTickManager;
	private final InPlayerGameEventListenersStorage gameEventsStorage;
	private final BlocksNetworkManager networkManager;
	private final TntHandler tntHandler;
	private final BlockPos zeroKeyPos;
	private final long sectionId;
	private final UUID id;
	private final PlayerFlagSet flags;
	private PlayerAccessor owner;
	private Level fallbackLevel;
	private long lifetime, subTickOrder;

	protected InPlayerManager(PlayerAccessor player) {
		this.owner = player;
		this.id = player.player().getUUID();
		this.fallbackLevel = player.player().level();
		this.sectionId = BlockPosBounds.getSectionByPlayer(player.player());
		if (this.sectionId == -1) {
			throw new IllegalStateException("Unbounded playerOwner without section! Level: " + this.fallbackLevel + " Player: " + player.player());
		}
		this.zeroKeyPos = InPlayerBlockPos.ZERO.boundedBlockPos(player.player());
		if (this.zeroKeyPos == null)
			throw new IllegalStateException("Null BlockPos in BlockInPlayer! Player section: " + MorphedPlayerSection.forLog(this) +
					" Level: " + this.fallbackLevel + " Player: " + player.player());
		this.flags = new PlayerFlagSet(this);
		this.flags.managerInit.setDirect(true);
		this.hitBoxCalculator = new HitBoxCalculator(this);
		this.cursor3D = new BlocksInPlayerCursor3D(this);
		this.storage = new BlocksInPlayerStorage(this, this::onBlockAdded, this::onBlockRemoved);
		this.networkManager = new BlocksNetworkManager(this, HEAVY_MODE);
		this.gameEventsStorage = new InPlayerGameEventListenersStorage(this);
		this.sectionHandler = new PlayerSectionHandler(this);
		this.blockEntityTickManager = new InPlayerBlockEntityTickManager(this);
		this.blockTicks = new PlayerTicks<>(this, (blockInPlayer, targetBlock) -> {
			if (blockInPlayer.getBlockState().is(targetBlock) && this.level() instanceof ServerLevel lv) {
				blockInPlayer.getBlockState().tick(lv, blockInPlayer.getPos(), lv.getRandom());
			}
		});
		this.fluidTicks = new PlayerTicks<>(this, (blockInPlayer, targetFluid) -> {
			if (blockInPlayer.shouldDoFluidAction() && blockInPlayer.getBlockState().getFluidState().is(targetFluid) && this.level() instanceof ServerLevel lv) {
				blockInPlayer.getBlockState().getFluidState().tick(lv, blockInPlayer.getPos(), blockInPlayer.getBlockState());
			}
		});
		this.tntHandler = new TntHandler(this);
		this.flags.managerInit.setDirect(false);
	}

	@Override
	public InPlayerManager getManager() {
		return this;
	}

	public static InPlayerManager createValidFor(PlayerAccessor pl) {
		return pl.player().level().isClientSide() ? new ClientInPlayerManager(pl) : new InPlayerManager(pl);
	}

	public void onLevelChange(Level newLv) {
		if (newLv != null && newLv != this.fallbackLevel) {
			this.fallbackLevel = newLv;
			this.storage.forEach(block -> {
				if (block.getBlockEntity() != null) {
					block.getBlockEntity().setLevel(newLv);
				}
			});
			this.tntHandler.changeLevel(newLv);
		}
	}

	public void onOwnerChanged(PlayerAccessor newOwner) {
		if (newOwner == null || newOwner == this.getOwner()) return;
		if (!this.player().getUUID().equals(newOwner.player().getUUID()))
			throw new IllegalArgumentException("Try connect to other playerOwner!");
		if (this.getOwner() != null) {
			this.getOwner().changeManager(null);
		}
		this.owner = newOwner;
		this.onLevelChange(newOwner.player().level());
		this.sectionHandler.onManagerUpdate();
		this.hitBoxCalculator.enqueueHitboxUpdate();
	}

	public void changeOwnerDirectTo(@Nullable ServerPlayer player) {
		this.assertOnDataSerialization();
		this.owner = PlayerAccessor.of(player);
		if (player != null) {
			this.fallbackLevel = player.level();
			if (this.owner.getManager() == this) return;
			this.owner.changeManager(this);
		}
	}

	public void changeLifeTime(long lifetime) {
		this.assertOnDataSerialization();
		this.lifetime = lifetime;
	}

	protected void assertOnDataSerialization() {
		if (!PlayerWorldSerializer.inSerializeProcess(this.id))
			throw new IllegalStateException();
	}

	public InPlayerManager assertOnInit() {
		if (!this.flags.managerInit.isTrue())
			throw new IllegalStateException();
		return this;
	}

	public PlayerFlagSet getFlags() {
		return this.flags;
	}

	public BlocksInPlayerStorage getBlocksStorage() {
		return this.storage;
	}

	public BlocksInPlayerCursor3D getCursor3D() {
		return this.cursor3D;
	}

	public PlayerSectionHandler getSectionHandler() {
		return this.sectionHandler;
	}

	public HitBoxCalculator getHitBoxCalculator() {
		return this.hitBoxCalculator;
	}

	public InPlayerGameEventListenersStorage getEventListenersStorage() {
		return this.gameEventsStorage;
	}

	public BlocksNetworkManager getNetworkManager() {
		return this.networkManager;
	}

	public TntHandler getTntHandler() {
		return this.tntHandler;
	}

	public PlayerAccessor getOwner() {
		return this.owner;
	}

	public BlockPos getZeroKey() {
		return this.zeroKeyPos;
	}

	public long getSectionId() {
		return this.sectionId;
	}

	public UUID getOwnerUUID() {
		return this.id;
	}

	public long getLifetime() {
		return this.lifetime;
	}

	public long nextTick() {
		return this.subTickOrder++;
	}

	public PlayerTicks<Block> getBlockTicks() {
		return this.blockTicks;
	}

	public PlayerTicks<Fluid> getFluidTicks() {
		return this.fluidTicks;
	}

	private Player player() {
		return this.owner.player();
	}

	public Level level() {
		if (this.owner == null) return this.fallbackLevel;
		return this.player().level();
	}

	protected InPlayerBlockEntityTickManager getBlockEntityTickManager() {
		return this.blockEntityTickManager;
	}

	public boolean isServer() {
		return !this.level().isClientSide();
	}

	public BlockInPlayer2 directLoadOrOverrideBlock(PlayerLoader loader, InPlayerBlockPos pos, BlockState state, BlockEntityType<?> type) {
		if (!loader.isReadyForLoadBlock()) throw new IllegalStateException();
		return new BlockInPlayer2(this, pos, state, this.storage::add, type);
	}

	protected void onBlockAdded(BlockInPlayer2 block) {
		this.hitBoxCalculator.onBlockAdded(block);
	}

	protected void onBlockRemoved(BlockInPlayer2 block) {
		this.hitBoxCalculator.onBlockRemoved(block);
	}

	public boolean setBlock(int inPlayerBlockPos, BlockState state, int flags) {
		return this.setBlock(inPlayerBlockPos, state, flags, 0);
	}

	public boolean setBlock(int inPlayerBlockPos, BlockState state, int flags, int suppressHitboxChange) {
		if (!InPlayerBlockPos.isValid(inPlayerBlockPos)) return false;
		if (this.tntHandler.getActiveTnt() != null) return false;

		boolean air = state.is(Blocks.AIR);
		BlockInPlayer2 old = this.storage.get(inPlayerBlockPos);
		if (old != null) {
			if (old.getBlockState() == state) return false;
			old.changeBlockState(state, flags);
			if (air) {
				this.storage.remove(inPlayerBlockPos);
			}
		} else {
			if (air) return false;
			BlockInPlayer2 block = new BlockInPlayer2(this, InPlayerBlockPos.decode(inPlayerBlockPos), state, this.storage::add, null);
			block.onPlace(state, Blocks.AIR.defaultBlockState(), flags);
		}
		if (suppressHitboxChange < 2) this.hitBoxCalculator.refreshPositions();
		if (suppressHitboxChange < 1) this.hitBoxCalculator.enqueueHitboxUpdate();
		return true;
	}

	public BannedBlock applyBlockMorph(BlockState state, CompoundTag tag, BannedBlock.Source source) {
		if (!(this.player() instanceof ServerPlayer serverPlayer))
			throw new IllegalStateException("Call only on server!");
		BannedBlock mess = BannedBlock.isBannedBlock(state, this.owner, source);
		if (mess != null) {
			return mess;
		}
		BlockState old = this.owner.getBlockState(InPlayerBlockPos.ZERO);
		boolean flag = state.equals(old);
		if (tag == null && flag)
			return BannedBlock.ALREADY_MORPHED;
		this.tntHandler.stop();
		serverPlayer.connection.send(new ClientBoundApplyBlockMorphPacket(state, this.owner).toVanillaClientbound());
		this.flags.morphProcess.setDirect(true);
		this.storage.forEach(block -> {
			int index = block.getOffset().asInt();
			if (index != ZERO_POS) this.blocksForErase.add(index);
		});
		this.blocksForErase.forEach((int pos) -> {
			this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			this.networkManager.enqueueBlockNetworkUpdate(pos);
		});
		this.blocksForErase.clear();
		this.setBlock(ZERO_POS, state, 3);
		BlockInPlayer2 block = this.storage.get(ZERO_POS);
		if (block != null && tag != null) {
			List<String> errs = block.getBlockEntity() != null ?
					this.guiTagLoader.loadInBlockEntity(block.getBlockEntity(), this.fallbackLevel.registryAccess(), tag) : null;
			if (errs != null) {
				serverPlayer.connection.send(ClientBoundServerBlockEntityTagPacket.createForError(errs.getFirst(), true).toVanillaClientbound());
			} else {
				CompoundTag newTag = this.owner.getTag(InPlayerBlockPos.ZERO);
				if (newTag != null && !newTag.equals(tag)) {
					serverPlayer.connection.send(ClientBoundServerBlockEntityTagPacket.createForError("", false).toVanillaClientbound());
				}
			}
		}
		state.getBlock().setPlacedBy(this.level(), this.zeroKeyPos, state, this.player(), new ItemStack(state.getBlock().asItem(), 1));
		this.networkManager.enqueueBlockNetworkUpdate(ZERO_POS);
		this.networkManager.runNetworkUpdates();
		this.flags.morphProcess.setDirect(false);
		return null;
	}

	public void receiveClientBlockMorph(BlockState blockState) {
		this.flags.morphProcess.setDirect(true);
		this.storage.forEach(block -> {
			int index = block.getOffset().asInt();
			if (index != ZERO_POS) this.blocksForErase.add(index);
		});
		this.blocksForErase.forEach((int pos) -> this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3));
		this.blocksForErase.clear();
		this.setBlock(ZERO_POS, blockState, 3);
		blockState.getBlock().setPlacedBy(this.level(), this.zeroKeyPos, blockState, this.player(), new ItemStack(blockState.getBlock().asItem(), 1));
		this.flags.morphProcess.setDirect(false);
	}

	public final void baseTick() {
		if (this.flags.serializingProcess.isTrue() || this.owner == null || this.owner.player().isDeadOrDying()) return;
		this.tick();
		this.fluidCollisionUpdate();
	}

	protected void tick() {
		this.hitBoxCalculator.tick();
		this.networkManager.tick();
		if (this.owner.isBlockomorphFullActive()) {
			this.lifetime++;
			this.blockEntityTickManager.tick();
			this.blockTicks.tick();
			this.fluidTicks.tick();
		}
		this.tntHandler.tick();
	}

	private void fluidCollisionUpdate() {
		BlockState state = this.owner.getBlockState(InPlayerBlockPos.ZERO);
		if (!this.player().getAbilities().flying && state.getBlock() instanceof LiquidBlock block) {
			if (this.level().getBlockState(this.player().blockPosition()).getBlock() == block) {
				Vec3 movement = this.player().getDeltaMovement();
				this.player().setDeltaMovement(movement.x, Math.min(1.8, movement.y + 0.3), movement.z);
				this.player().resetFallDistance();
			}
		}
	}
}
