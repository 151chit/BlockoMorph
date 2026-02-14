package net.blockomorph.mixins.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.blockomorph.network.ClientBoundApplyBlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.network.ClientBoundServerBlockEntityTagPacket;
import net.blockomorph.utils.*;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.gameEvent.PlayerDynamicGameEventListener;
import net.blockomorph.utils.gameEvent.SafeIterableStorage;
import net.blockomorph.utils.MorphedPlayerRenderer;
import net.blockomorph.utils.tick.InPlayerBlockEntityTickManager;
import net.blockomorph.utils.tick.PlayersTickManager;
import net.blockomorph.utils.tick.PlayersTicks;
import net.blockomorph.utils.tnt.TntHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccessor {
	@Shadow
	protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose p_294172_);

	@Shadow @Final private Abilities abilities;
	@Unique private boolean init$bmMorph;
	private final TntHandler TNT_HANDLER = new TntHandler(this);
	private final HitBoxCalculator HITBOX_HANDLER = new HitBoxCalculator(this);
	private final ConcurrentHashMap<InPlayerBlockPos, BlockInPlayer2> blocksData = new ConcurrentHashMap<>();
	private final Map<InPlayerBlockPos, BlockInPlayer2> unmodifiableBlocksData = Collections.unmodifiableMap(this.blocksData);
	private final Set<InPlayerBlockPos> updates = ConcurrentHashMap.newKeySet();
	private final ObjectLinkedOpenHashSet<InPlayerBlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
	private final InPlayerBlockEntityTickManager blockEntityTickManager = new InPlayerBlockEntityTickManager(this);
	private PlayerDynamicGameEventListener listenerStorage;
	@Unique private boolean onLoadingBlocks;
	@Unique private boolean breakingMode;
	@Unique private boolean unContextedBreakingMode;

	public boolean setBlockState(InPlayerBlockPos pos, BlockState state, int flags) {
		BlockInPlayer2 old;
		if (state.getBlock() == Blocks.AIR && this.isBreaking() && InPlayerBlockPos.ZERO.equals(pos)) {
			this.setBlockState(pos, Blocks.VOID_AIR.defaultBlockState(), flags);
		} else if (pos == null || !pos.isValid() || ((old = this.blocksData.get(pos)) != null && old.getBlockState() == state)) {
			return false;
		} else if (state.getBlock() == Blocks.AIR) {
			BlockInPlayer2 block = this.blocksData.get(pos);
			if (block != null)
				block.changeBlockState(state, flags);
			this.blocksData.remove(pos);
		} else if (blocksData.containsKey(pos)) {
			this.blocksData.get(pos).changeBlockState(state, flags);
		} else {
			BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer), null);
			block.checkUpdates(flags, Blocks.AIR.defaultBlockState());
			block.onPlace(state, Blocks.AIR.defaultBlockState(), flags);
		}
		HITBOX_HANDLER.recalculatePositions();
		this.refreshDimensions();
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

			return ent.getBlockEntity().saveWithoutMetadata(this.registryAccess());
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

	public Map<InPlayerBlockPos, BlockInPlayer2> getBlocksData2() {
		return this.unmodifiableBlocksData;
	}

	public void prepareSync(InPlayerBlockPos pos) {
		if (!this.level().isClientSide) {
			this.updates.add(pos);
		}
	}

	public void prepareSync(InPlayerBlockEventData data) {
		if (!this.level().isClientSide()) {
			this.blockEvents.add(data);
		}
	}

	public void breakingModeStart(boolean yes) {
		this.unContextedBreakingMode = yes;
		if (Config.get().playerDieAfterDestroy.getValue() || !yes)
			this.breakingMode = yes;
	}

	public HitBoxCalculator getHitBoxHandler() {
		return HITBOX_HANDLER;
	}

	public boolean isBreaking() {
		return this.breakingMode;
	}

	public boolean isUnContextedBreaking() {
		return this.unContextedBreakingMode;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains("BlockoMorph")) {
			CompoundTag data = tag.getCompound("BlockoMorph").orElseThrow();
			CompoundTag ticks = null;
			if (data.contains("ticks")) {
				ticks = data.getCompound("ticks").orElseThrow();
				data.remove("ticks");
			}
			this.loadBlockData(data, null, true);
			if (ticks != null) {
				this.loadSavedTicks(ticks);
			}
		} else if (tag.contains("BlockMorph")) {
			this.oldDataHandle(tag.getCompound("BlockMorph").orElseThrow());
		}
	}

	public void loadBlockData(CompoundTag blockomorph, @Nullable ClientBoundMorphUpdatePacket client, boolean first) {
		if (first) {
			this.blocksData.values().forEach(BlockInPlayer2::clearBlockEntity);
			this.blocksData.clear();
		}
		for (String key : blockomorph.keySet()) {
			InPlayerBlockPos pos = InPlayerBlockPos.parseBlockPos(key);
			if (pos != null) {
				CompoundTag tg = blockomorph.getCompound(key).orElseThrow();
				BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tg.getCompoundOrEmpty("BlockState"));
				CompoundTag tags = tg.getCompoundOrEmpty("BlockEntityTag");
				BlockEntityType<?> blockEntityType = tags.read("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()).orElse(null);
				BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer), blockEntityType);
				if (client != null) {
					block.handleClientTag(tags, client);
				} else {
					block.loadNBT(tags);
				}
			}
		}
		HITBOX_HANDLER.recalculatePositions();
		this.refreshDimensions();
	}

	private void loadSavedTicks(CompoundTag ticks) {
		if (this.player() instanceof ServerPlayer pl) {
			long gameTime = PlayersTickManager.getGameTime();
			for (PlayersTicks<?> playersTicks : PlayersTickManager.getTicks()) {
				this.decodePartialTicks(gameTime, pl, ticks, playersTicks);
			}
		}
	}

	private <T> void decodePartialTicks(long gameTime, ServerPlayer pl, CompoundTag ticks, PlayersTicks<T> ticksStorage) {
		CompoundTag ticksTag = ticks.getCompoundOrEmpty(ticksStorage.name);
		if (!ticksTag.isEmpty()) {
			Codec<SavedTick<T>> tickCodec = SavedTick.codec(ticksStorage.codecSupplier.get());
			try {
				HashMap<ChunkPos, List<SavedTick<T>>> ticksMap = new HashMap<>();
				BlockPos zeroAbsolute = InPlayerBlockPos.ZERO.boundedBlockPos(this.player());
				if (zeroAbsolute == null) throw new IllegalArgumentException("Key-blockpos is null?!");
				for (String key : ticksTag.keySet()) {
					String[] xz = key.split(" ");
					if (xz.length != 2) throw new IllegalArgumentException("Irregular chunkpos format: " + key);
					ChunkPos pos = new ChunkPos(Integer.parseInt(xz[0]), Integer.parseInt(xz[1]));
					ListTag savedTicks = ticksTag.getListOrEmpty(key);
					ticksMap.put(pos, savedTicks.stream().map(tag -> tickCodec.parse(NbtOps.INSTANCE, tag).getOrThrow()).map(savedTick -> {
						BlockPos absolute = zeroAbsolute.offset(savedTick.pos());
						return new SavedTick<>(savedTick.type(), absolute, savedTick.delay(), savedTick.priority());
					}).toList());
				}
				ticksStorage.replaceOrRegisterForPlayer(pl, ticksMap, gameTime);
			} catch (Exception ex) {
				MorphUtils.LOGGER.error("An unexpected error occurred while loading player {}'s ticks, {} not loaded!", this.player(), ticksStorage.name);
				MorphUtils.LOGGER.error("Error: ", ex);
				ticksStorage.replaceOrRegisterForPlayer(pl, null, gameTime);
			}
		} else {
			ticksStorage.replaceOrRegisterForPlayer(pl, null, gameTime);
		}
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
					CompoundTag servTag = ent.saveWithoutMetadata(this.registryAccess());
					ResourceLocation blockEntityTypeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ent.getType());
					if (blockEntityTypeId != null)
						servTag.put("id", StringTag.valueOf(blockEntityTypeId.toString()));
					blockTag = client ? ent.getUpdateTag(this.registryAccess()) : servTag;
				}
				tg.put("BlockEntityTag", blockTag);
				blocks.put(pos.string(), tg);
			} catch (Exception e) {
				MorphUtils.LOGGER.error("An error occurred while saving morphed player data on pos: {} for block: {} on player: {}", pos, data.getBlockState(), this.getName().getString(), e);
			}
		});
		return blocks;
	}

	private CompoundTag getSavedTicks() {
		CompoundTag ticks = new CompoundTag();
		if (this.player() instanceof ServerPlayer pl) {
			long gameTime = PlayersTickManager.getGameTime();
			for (PlayersTicks<?> playersTicks : PlayersTickManager.getTicks()) {
				this.encodePartialTicks(gameTime, pl, ticks, playersTicks);
			}
		}
		return ticks;
	}

	private <T> void encodePartialTicks(long gameTime, ServerPlayer pl, CompoundTag ticks, PlayersTicks<T> ticksStorage) {
		HashMap<ChunkPos, LevelChunkTicks<T>> ticksMap = ticksStorage.getTickersForPlayer(pl);
		Long2LongMap offset = PlayerMorphedSection.absoluteToOffset(this);
		BlockPos zeroAbsolute = InPlayerBlockPos.ZERO.boundedBlockPos(this.player());
		if (ticksMap != null && offset != null && zeroAbsolute != null) {
			CompoundTag ticksTag = new CompoundTag();
			ticks.put(ticksStorage.name, ticksTag);
			Codec<SavedTick<T>> tickCodec = SavedTick.codec(ticksStorage.codecSupplier.get());
			for (Map.Entry<ChunkPos, LevelChunkTicks<T>> entry : ticksMap.entrySet()) {
				ListTag chunkTag = new ListTag();
				ChunkPos forSavePos = new ChunkPos(offset.get(entry.getKey().toLong()));
				ticksTag.put(forSavePos.x + " " + forSavePos.z, chunkTag);
				for (SavedTick<T> savedTick : entry.getValue().pack(gameTime)) {
					BlockPos offsetted = new BlockPos(savedTick.pos().subtract(zeroAbsolute));
					SavedTick<T> offsettedSavedTick = new SavedTick<>(savedTick.type(), offsetted, savedTick.delay(), savedTick.priority());
					chunkTag.add(tickCodec.encodeStart(NbtOps.INSTANCE, offsettedSavedTick).getOrThrow());
				}
			}
		} else {
			MorphUtils.LOGGER.error("An unexpected error occurred while saving player {}'s ticks, {} were not saved!", this.player(), ticksStorage.name);
			MorphUtils.LOGGER.error("PlayerChunkTicks availability: {}", ticksMap != null);
			MorphUtils.LOGGER.error("Keypos encoder map availability: {}", offset != null);
			MorphUtils.LOGGER.error("Keypos availability: {}", zeroAbsolute != null);
		}
	}

	public boolean isOnLoadingBlocks() {
		return this.onLoadingBlocks;
	}

	public void setOnLoadingBlocks(boolean yes) {
		this.onLoadingBlocks = yes;
	}

	private void oldDataHandle(CompoundTag tag) {
		if (!this.level().isClientSide) {
			BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompoundOrEmpty("BlockState"));
			CompoundTag tags = tag.getCompoundOrEmpty("Tags");
			InPlayerBlockPos pos = InPlayerBlockPos.ZERO;
			BlockInPlayer2 block = new BlockInPlayer2(this, pos, state, (blockInPlayer) -> this.blocksData.put(pos, blockInPlayer), null);
			block.loadNBT(tags);
			HITBOX_HANDLER.recalculatePositions();
			this.refreshDimensions();
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		CompoundTag tg = this.saveBlockData(false);
		if (!tg.isEmpty()) {
			CompoundTag ticks = this.getSavedTicks();
			tg.put("ticks", ticks);
			tag.put("BlockoMorph", tg);
		}
	}

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void attack(Entity entity, CallbackInfo ci) {
		if (Config.get().hitReaction.getValue().hand) {
			return;
		}
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

	public Set<InPlayerBlockPos> getUpdates() {
		return Collections.unmodifiableSet(this.updates);
	}

	public void getBlocksData2InArea(AABB box, TriConsumer<InPlayerBlockPos, BlockInPlayer2, Vec3> action) {
		if (action != null && box != null) {
			Vec3 realPos = MorphUtils.getRealBlockPos(this, InPlayerBlockPos.ZERO);
			int minX = Mth.floor(box.minX - realPos.x);
			int minY = Mth.floor(box.minY - realPos.y);
			int minZ = Mth.floor(box.minZ - realPos.z);
			int maxX = Mth.floor(box.maxX - realPos.x);
			int maxY = Mth.floor(box.maxY - realPos.y);
			int maxZ = Mth.floor(box.maxZ - realPos.z);
			for (int x = Math.max(-14, minX); x <= Math.min(14, maxX); x++) {
				for (int y = Math.max(0, minY); y <= Math.min(30, maxY); y++) {
					for (int z = Math.max(-14, minZ); z <= Math.min(14, maxZ); z++) {
						InPlayerBlockPos pos = InPlayerBlockPos.get(x, y, z);
						BlockInPlayer2 block;
						if ((block = this.blocksData.get(pos)) != null) {
							action.accept(pos, block, realPos.add(new Vec3(x, y, z)));
						}
					}
				}
			}
		}
	}

	public void sendAllContentToPlayer(ServerPlayer target) {
		CompoundTag blocks = this.saveBlockData(true);
		CompoundTag buffer = new CompoundTag();
		List<ClientBoundMorphUpdatePacket> packets = new ArrayList<>();
		boolean first = true;

		for (String key : blocks.keySet()) {
			CompoundTag block = blocks.getCompound(key).orElseThrow();
			if (!block.isEmpty()) {
				buffer.put(key, block);
				if (buffer.sizeInBytes() > 524288) {
					buffer.remove(key);
					packets.add(new ClientBoundMorphUpdatePacket(this, buffer, first));
					first = false;
					buffer = new CompoundTag();
					buffer.put(key, block);
				}
			}
		}
		packets.add(new ClientBoundMorphUpdatePacket(this, buffer, first));
		for (ClientBoundMorphUpdatePacket packet : packets) {
			MorphUtils.sendPlayer(packet, target);
		}
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
				var packet = ent.getUpdatePacket();
				if (packet != null)
					this.sendNearby(packet);
			} catch (Exception e) {
				MorphUtils.LOGGER.error("An error occurred while sending morphed player data on pos: {} for block: {} on player: {}", block.getOffset(), block.getBlockState(), this.getName().getString(), e);
			}
		}
	}

	private void runUpdatesBlockEvents() {
		if (this.blockEvents.isEmpty())
			return;
		while (!this.blockEvents.isEmpty()) {
			InPlayerBlockEventData blockData = this.blockEvents.removeFirst();
			BlockInPlayer2 block = this.blocksData.get(blockData.inPlayerPosOfBlock());
			BlockEventData data = blockData.data();
			if (block != null && block.getBlockState().is(data.block())) {
				boolean result = block.getBlockState().triggerEvent(this.level(), data.pos(), data.paramA(), data.paramB());
				if (result)
					this.sendNearby(new ClientboundBlockEventPacket(data.pos(), data.block(), data.paramA(), data.paramB()));
			}
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void tick(CallbackInfo ci) {
		if (!this.level().isClientSide) {
			this.runUpdates();
			this.runUpdatesBlockEvents();
		}
		if (this.isFullActive()) {
			ProfilerFiller profiler = Profiler.get();
			profiler.push("blockInMorphedPlayerTick");
			this.blockEntityTickManager.tick();
			profiler.pop();
		}
		TNT_HANDLER.tick();
		this.fluidCollisionUpdate();
		if (!this.init$bmMorph) {
			this.init$bmMorph = true;
			this.getEntityData().set(DATA_POSE, this.getPose(), true);
		}
	}

	private void fluidCollisionUpdate() {
		BlockState state = this.getBlockState(InPlayerBlockPos.ZERO);
		if (!this.abilities.flying && state.getBlock() instanceof LiquidBlock block) {
			if (this.level().getBlockState(this.blockPosition()).getBlock() == block) {
				Vec3 movement = this.getDeltaMovement();
				this.setDeltaMovement(movement.x, Math.min(1.8, movement.y + 0.3), movement.z);
				this.resetFallDistance();
			}
		}
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true) //TODO
	public void causeFallDamage(double d, float f, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
		if (this.isActive() && this.level() instanceof ServerLevel lv) {
			cir.cancel();
			if (!(this.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof AnvilBlock)) {
				return;
			}
			int i = Mth.ceil(d - 1.0f);
			if (i < 0) {
				return;
			}
			Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
			DamageSource damageSource22 = this.damageSources().anvil(this);
			float h = Math.min(Mth.floor((float) i * 2), 40);
			this.level().getEntities(this, this.getBoundingBox(), predicate).forEach(entity -> entity.hurtServer(lv, damageSource22, h));
		}
	}

	@Nullable
	public BannedBlock applyBlockMorph(BlockState state, CompoundTag tag, BannedBlock.Source source) {
		BannedBlock mess = BannedBlock.isBannedBlock(state, this, source);
		if (mess != null) {
			return mess;
		}
		BlockState old = this.getBlockState(InPlayerBlockPos.ZERO);
		boolean flag = state.equals(old);
		if (tag == null && flag)
			return BannedBlock.ALREADY_MORPHED;
		MorphUtils.sendPlayer(new ClientBoundApplyBlockMorphPacket(state, this), (ServerPlayer) this.player());
		this.onLoadingBlocks = true;
		for (InPlayerBlockPos pos : this.blocksData.keySet()) {
			if (!pos.equals(InPlayerBlockPos.ZERO)) {
				this.setBlockState(pos, Blocks.AIR.defaultBlockState(), 3);
				this.updates.add(pos);
			}
		}
		this.setBlockState(InPlayerBlockPos.ZERO, state, 3);
		BlockInPlayer2 block = this.blocksData.get(InPlayerBlockPos.ZERO);
		if (block != null && tag != null) {
			Throwable e = block.loadNBT(tag);
			ServerPlayer serverPlayer = (ServerPlayer) this.player();
			if (e != null) {
				MorphUtils.sendPlayer(ClientBoundServerBlockEntityTagPacket.createForError(Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()), true), serverPlayer);
			} else {
				CompoundTag newTag = this.getTag(InPlayerBlockPos.ZERO);
				if (!newTag.equals(tag)) {
					MorphUtils.sendPlayer(ClientBoundServerBlockEntityTagPacket.createForError("", false), serverPlayer);
				}
			}
		}
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

	@Override
	public InPlayerBlockEntityTickManager getBlockEntityTickManager() {
		return this.blockEntityTickManager;
	}

	@Override @Nullable
	public PlayerDynamicGameEventListener getListenersStorage() {
		if (this.listenerStorage == null && this.player() instanceof ServerPlayer player)
			this.listenerStorage = new PlayerDynamicGameEventListener(player, new SafeIterableStorage<>());
		return this.listenerStorage;
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
			int progress = -1;
			for (BlockInPlayer2 block : this.blocksData.values()) {
				int k = MorphedPlayerRenderer.getBrakeProgress(block.getPos());
				if (k > progress) progress = k;
			}
			return progress;
		}
		return -1;
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

	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState modifyState(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		var entry = MorphUtils.getLiquidOnPos(this, this.position().add(0, 0.9, 0));
		if (entry != null) {
			return entry.getValue().getBlockState().getFluidState();
		}
		return original.call(instance, blockPos);
	}

	public PlayerMixin() {
		super(null, null);
	}
}
