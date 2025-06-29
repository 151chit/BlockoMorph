package net.blockomorph.mixins.main;

import net.blockomorph.BlockomorphServer;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.network.ClientBoundServerBlockEntityTagPacket;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.utils.*;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.tnt.TntHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccessor {
	@Shadow protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

	private static final EntityDataAccessor<Integer> TNT_PROGRESS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
	private final TntHandler TNT_HANDLER = new TntHandler(this, this.entityData, TNT_PROGRESS);
	private final HitBoxCalculator HITBOX_HANDLER = new HitBoxCalculator(this);
	private final ConcurrentHashMap<InPlayerBlockPos, BlockInPlayer2> blocksData = new ConcurrentHashMap<>();
	private final List<InPlayerBlockPos> updates = new CopyOnWriteArrayList<>();
	private final ConcurrentHashMap<InPlayerBlockPos, BlockEventData> blocksEventsToTick = new ConcurrentHashMap<>();
	private boolean onLoadingBlocks;
	private boolean breakingMode;

	public boolean setBlockState(InPlayerBlockPos pos, BlockState state, boolean updateInternal) {
		if (state.getBlock() == Blocks.AIR && this.isBreaking() && pos.equals(InPlayerBlockPos.ZERO)) {
			this.setBlockState(pos, Blocks.VOID_AIR.defaultBlockState(), updateInternal);
		} else if (pos == null || !pos.isValid()) {
			return false;
		} else if (state.getBlock() == Blocks.AIR) {
			BlockInPlayer2 block = this.blocksData.get(pos);
			if (block != null)
				block.changeBlockState(state, updateInternal);
			this.blocksData.remove(pos);
		} else if (blocksData.containsKey(pos)) {
			this.blocksData.get(pos).changeBlockState(state, updateInternal);
		} else {
			BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer));
			block.onPlace(state, Blocks.AIR.defaultBlockState(), updateInternal);
		}
		HITBOX_HANDLER.recalculatePositions();
		this.refreshDimensions();

		if (this.level().isClientSide && pos.equals(InPlayerBlockPos.ZERO))
			this.clientUpdate();
		return true;
	}

	@Nullable
	public BlockEntity getBlockEntity(InPlayerBlockPos pos) {
		if (this.blocksData == null)
			return null;
		BlockInPlayer2 bl = this.blocksData.get(pos);
		if (bl != null)
			return bl.getBlockEntity();
		return null;
	}

	public CompoundTag getTag(InPlayerBlockPos pos) {
		BlockInPlayer2 ent = this.blocksData.get(pos);
		if (ent != null && ent.getBlockEntity() != null) {
			if (this.level().isClientSide)
				return ent.getServerTag();
			return ent.getBlockEntity().saveWithoutMetadata(this.level().registryAccess());
		}
		return new CompoundTag();
	}

	public BlockState getBlockState(InPlayerBlockPos pos) {
		if (this.blocksData == null)
			return Blocks.AIR.defaultBlockState();
		BlockInPlayer2 bl = this.blocksData.get(pos);
		if (bl != null)
			return bl.getBlockState();
		return Blocks.AIR.defaultBlockState();
	}

	public HashMap<InPlayerBlockPos, BlockInPlayer2> getBlocksData2() {
		return new HashMap<>(this.blocksData);
	}

	public void prepareSync(InPlayerBlockPos pos) {
		if (!this.level().isClientSide && !this.updates.contains(pos)) {
			this.updates.add(pos);
		}
	}

	public void prepareSync(InPlayerBlockPos pos, BlockEventData data) {
		if (!this.level().isClientSide && !this.blocksEventsToTick.containsKey(pos)) {
			this.blocksEventsToTick.put(pos, data);
		}
	}

	public void breakingModeStart(boolean yes) {
		if ((Boolean) Config.getInstance().getValue("playerDieAfterDestroy") || !yes)
			this.breakingMode = yes;
	}

	public boolean isBreaking() {
		return this.breakingMode;
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"), cancellable = true)
	protected void defineSynchedData(SynchedEntityData.Builder entityData, CallbackInfo ci) {
		entityData.define(TNT_PROGRESS, -1);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains("BlockoMorph")) {
			this.loadBlockData(tag.getCompound("BlockoMorph"), null);
		} else if (tag.contains("BlockMorph")) {
			this.oldDataHandle(tag.getCompound("BlockMorph"));
		}
	}

	public void loadBlockData(CompoundTag blockomorph, @Nullable ClientBoundMorphUpdatePacket client) {
		this.blocksData.clear();
		for (String key : blockomorph.getAllKeys()) {
			InPlayerBlockPos pos = InPlayerBlockPos.parseBlockPos(key);
			if (pos != null) {
				CompoundTag tg = blockomorph.getCompound(key);
				BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tg.getCompound("BlockState"));
				CompoundTag tags = tg.getCompound("BlockEntityTag");
				BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer));
				if (client != null) {
					block.setServerTag(tg.getCompound("ServerTag"));
					block.handleClientTag(tags, client);
				} else {
					block.loadNBT(tags);
				}
			}
		}
		HITBOX_HANDLER.recalculatePositions();
		this.refreshDimensions();
	}

	public CompoundTag saveBlockData(boolean client) {
		CompoundTag blocks = new CompoundTag();
		this.blocksData.forEach((pos, data) -> {
			try {
				CompoundTag tg = new CompoundTag();
				tg.put("BlockState", NbtUtils.writeBlockState(data.getBlockState()));
				BlockEntity ent = data.getBlockEntity();
				CompoundTag blockTag = new CompoundTag();
				if (ent != null) {
					CompoundTag servTag = ent.saveWithoutMetadata(this.level().registryAccess());
					if (client) {
						tg.put("ServerTag", servTag);
					}
					blockTag = client ? ent.getUpdateTag(this.level().registryAccess()) : servTag;

				}
				tg.put("BlockEntityTag", blockTag);
				blocks.put(pos.string(), tg);
			} catch (Exception e) {
				BlockomorphServer.LOGGER.error("An error occurred while saving morphed player data on pos: " + pos + " for block: " + data.getBlockState() + " on player: " + this.getName().getString(), e);
			}
		});
		return blocks;
	}

	public boolean isOnLoadingBlocks() {
		return this.onLoadingBlocks;
	}

	private void oldDataHandle(CompoundTag tag) {
		if (!this.level().isClientSide) {
			BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
			CompoundTag tags = tag.getCompound("Tags");
			InPlayerBlockPos pos = InPlayerBlockPos.ZERO;
			BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer));
			block.loadNBT(tags);
			HITBOX_HANDLER.recalculatePositions();
			this.refreshDimensions();
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		CompoundTag tg = this.saveBlockData(false);
		if (!tg.isEmpty()) {
			tag.put("BlockoMorph", tg);
		}
	}

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void attack(Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl && pl.isActive())
			ci.cancel();
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	public void updatePose(CallbackInfo ci) {
		this.getSleepingPos().ifPresent((pos) -> {
			if (InPlayerBlockPos.isMorphedPlayerX(pos.getX()) && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
				// fix to modded beds, that not passed Entity#canEnterPose and using custom render rotator
				this.setPose(Pose.SLEEPING);
				ci.cancel();
			}
		});
	}

	public void sendNearby(Packet<?> packet) {
		if (this.level() instanceof ServerLevel lv) {
			ServerChunkCache cache = lv.getChunkSource();
			cache.broadcastAndSend(this.player(), packet);
		}
	}

	public List<InPlayerBlockPos> getUpdates() {
		return new ArrayList<>(this.updates);
	}

	private void runUpdates() {
		if (this.updates.isEmpty())
			return;
		this.updates.forEach((realPos) -> {
			BlockInPlayer2 block = this.blocksData.get(realPos);
			if (block == null) {
				BlockPos pos = realPos.boundedBlockPos(this.player());
				if (pos != null)
					this.sendNearby(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
			} else {
				this.sendBockData(block);
			}
		});
		this.updates.clear();
	}

	private void sendBockData(BlockInPlayer2 block) {
		this.sendNearby(new ClientboundBlockUpdatePacket(block.getPos(), block.getBlockState()));
		BlockEntity ent = block.getBlockEntity();
		if (ent != null) {
			try {
				if (ent.getUpdatePacket() != null)
					this.sendNearby(ent.getUpdatePacket());
				MorphUtils.sendPlayer(new ClientBoundServerBlockEntityTagPacket(block.getOffset(), ent.saveWithoutMetadata(this.level().registryAccess())), (ServerPlayer) this.player());
			} catch (Exception e) {
				BlockomorphServer.LOGGER.error("An error occurred while sending morphed player data on pos: " + block.getOffset() + " for block: " + block.getBlockState() + " on player: " + this.getName().getString(), e);
			}
		}
	}

	private void runUpdatesBlockEvents() {
		if (this.blocksEventsToTick.isEmpty())
			return;
		this.blocksEventsToTick.forEach((realPos, data) -> {
			BlockInPlayer2 block = this.blocksData.get(realPos);
			if (block != null) {
				block.getBlockState().triggerEvent(this.level(), data.pos(), data.paramA(), data.paramB());
				this.sendNearby(new ClientboundBlockEventPacket(data.pos(), data.block(), data.paramA(), data.paramB()));
			}
		});
		this.blocksEventsToTick.clear();
	}

	@Inject(method = "tick", at = @At("TAIL"), cancellable = true)
	public void tick(CallbackInfo ci) {
		if (!this.level().isClientSide) {
			this.runUpdates();
			this.runUpdatesBlockEvents();
		}
        for (BlockInPlayer2 block : this.blocksData.values()) {
			block.tick();
		}
		TNT_HANDLER.tick();
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true) //TODO
	public void causeFallDamage(float f, float g, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
		if (this.isActive()) {
			cir.cancel();
			if (!(this.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof AnvilBlock)) {
				return;
			}
			int i = Mth.ceil(f - 1.0f);
			if (i < 0) {
				return;
			}
			Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
			DamageSource damageSource22 = this.damageSources().anvil(this);
			float h = Math.min(Mth.floor((float) i * 2), 40);
			this.level().getEntities(this, this.getBoundingBox(), predicate).forEach(entity -> entity.hurt(damageSource22, h));
		}
	}

	@Nullable
	public MorphUtils.BannedBlock applyBlockMorph(BlockState state, CompoundTag tag) {
		MorphUtils.BannedBlock mess = MorphUtils.isBannedBlock(state, null);
		if (mess != null) {
			return mess;
		}
		BlockState old = this.getBlockState(InPlayerBlockPos.ZERO);
		boolean flag = state.equals(old);
		if ((tag == null || tag.isEmpty()) && flag)
			return MorphUtils.BannedBlock.SAME;
		this.onLoadingBlocks = true;
		for (InPlayerBlockPos pos : this.blocksData.keySet()) {
			if (!pos.equals(InPlayerBlockPos.ZERO)) {
				this.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
				this.updates.add(pos);
			}
		}
		this.setBlockState(InPlayerBlockPos.ZERO, state, false);
		BlockInPlayer2 block = this.blocksData.get(InPlayerBlockPos.ZERO);
		if (block != null && tag != null && !tag.isEmpty())
			block.loadNBT(tag);
		BlockPos pos = InPlayerBlockPos.ZERO.boundedBlockPos(this.player());
		if (pos != null) {
			state.getBlock().setPlacedBy(this.level(), pos, state, this, new ItemStack(state.getBlock().asItem(), 1));
		}
		this.updates.add(InPlayerBlockPos.ZERO);
		this.runUpdates();
		this.onLoadingBlocks = false;
		if (!this.level().isClientSide) {
			TNT_HANDLER.stopRunning();
		}
		return null;
	}

	public PrimedTnt getTnt() {
		return TNT_HANDLER.getTnt();
	}

	public void setTnt() {
		TNT_HANDLER.runTnt();
	}

	public TntHandler getTntHandler() {
		return TNT_HANDLER;
	}

	public boolean isActive() {
		return this.getBlockState(InPlayerBlockPos.ZERO).getBlock() != Blocks.AIR;
	}

	public boolean isFullActive() {
		return this.isActive() && TNT_HANDLER.getTnt() == null;
	}

	public InPlayerBlockPos minPos() {
		return HITBOX_HANDLER.getMinPos();
	}

	public InPlayerBlockPos maxPos() {
		return HITBOX_HANDLER.getMaxPos();
	}

	public int getBiggestProgress() {
		if (this.blocksData != null) {
			BlockInPlayer2 main = this.blocksData.get(InPlayerBlockPos.ZERO);
			if (main != null)
				return MorphedPlayerRenderer.getBrakeProgress(main.getPos());
		}
		return -1;
	}

	public HitBoxCalculator getHitBoxHandler() {
		return HITBOX_HANDLER;
	}

	@Environment(EnvType.CLIENT)
	public void clientUpdate() {
		if (Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc && Minecraft.getInstance().player == (PlayerAccessor)this)
			sc.morphUpdate(this.getBlockState(InPlayerBlockPos.ZERO));
	}

	public VoxelShape getShape(InPlayerBlockPos offset, @Nullable Vec3 realPos) {
		BlockInPlayer2 block = this.getBlocksData2().get(offset);
		if (block == null) return Shapes.empty();
		VoxelShape shp = block.getBlockState().getCollisionShape(this.level(), block.getPos(), CollisionContext.of(this));
		Vec3 rl;
		if (realPos != null) {
			rl = realPos;
		} else {
			rl = MorphUtils.getRealBlockPos(this, offset);
		}
		return shp.move(rl.x, rl.y, rl.z);
	}

	public VoxelShape getRenderShape(InPlayerBlockPos pos, Player collisser) {
		BlockInPlayer2 block = this.getBlocksData2().get(pos);
		if (block == null) return Shapes.empty();
		BlockState state = block.getBlockState();
		VoxelShape shape = state.getShape(this.level(), block.getPos(), CollisionContext.of(collisser));
		return shape.move(pos.getX(), pos.getY(), pos.getZ());
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	protected void getDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
		if (this.isActive())
			cir.setReturnValue(null);
	}

	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	protected void getHurtSound(DamageSource damage, CallbackInfoReturnable<SoundEvent> cir) {
		if (this.isActive())
			cir.setReturnValue(null);
	}

	@Inject(method = "getFallSounds", at = @At("HEAD"), cancellable = true)
	protected void getFallSounds(CallbackInfoReturnable<Fallsounds> cir) {
		if (this.isActive())
			cir.setReturnValue(new Fallsounds(SoundEvents.EMPTY, SoundEvents.EMPTY));
	}

	@Inject(method = "maybeBackOffFromEdge", at = @At(value = "RETURN"), cancellable = true)
	public void fixMovement(Vec3 originalVector, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
		if (this.isActive() && (moverType == MoverType.PLAYER || moverType == MoverType.SELF)) {
			MovementCalculator calculator = new MovementCalculator(this);
			calculator.calculateEnterCorrection(originalVector);
		}
	}

	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (this.isActive()) {
			cir.setReturnValue(HITBOX_HANDLER.calculateDimensions());
		}
	}

	public PlayerMixin() {
		super(null, null);
	}
}
