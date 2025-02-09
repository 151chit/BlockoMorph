package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.g;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

import net.blockomorph.utils.*;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.BlockomorphMod;

import javax.annotation.Nullable;

import java.util.function.Predicate;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccessor {
	private static final EntityDataAccessor<CompoundTag> DATA_BlockMorph = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<CompoundTag> BRAKE_PROGRESS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
	private final List<BlockBracker> brackers = new CopyOnWriteArrayList();
	private HashMap<BlockPos, BlockState> blocks = new HashMap();
	private boolean readyForDestroy = true;
	private boolean afterExplodeFallProtect;
	private BlockPos minPos = new BlockPos(0, 0, 0);
	private BlockPos maxPos = new BlockPos(0, 0, 0);
	private PrimedTnt tnt;

	public PlayerMixin(EntityType<? extends LivingEntity> type, Level world) {
		super(type, world);
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"), cancellable = true)
	protected void defineSynchedData(CallbackInfo ci) {
		CompoundTag morphblocktag = new CompoundTag();
		morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
		morphblocktag.putBoolean("MultiBlock", false);
		this.entityData.define(DATA_BlockMorph, morphblocktag);
		this.entityData.define(BRAKE_PROGRESS, new CompoundTag());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains("BlockMorph")) {
			this.entityData.set(DATA_BlockMorph, tag.getCompound("BlockMorph"), true);
		} else {
			CompoundTag morphblocktag = new CompoundTag();
			morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
			morphblocktag.putBoolean("MultiBlock", false);
			this.entityData.set(DATA_BlockMorph, morphblocktag, true);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		tag.put("BlockMorph", this.entityData.get(DATA_BlockMorph));
	}

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void attack(Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerAccessor pl && pl.isActive())
			ci.cancel();
	}

	@Inject(method = "tick", at = @At("TAIL"), cancellable = true)
	public void tick(CallbackInfo ci) {
		for (Iterator<BlockBracker> iterator = this.brackers.iterator(); iterator.hasNext();) {
			iterator.next().tick();
		}
		this.tntTick();
	}

	private void tntTick() {
		if (this.tnt != null) {
			try {
			    tnt.setPosRaw(this.getX(), this.getY() + 0.06125D, this.getZ());
			    tnt.setOldPosAndRot();
				tnt.tick();
				if (!this.level().isClientSide) {
					this.setFuse(tnt.getFuse());
					if (!tnt.isAlive()) {
						//tnt = null;
					    MorphUtils.destroy(this, null);
					    tnt = null;
					    this.setFuse(-1);
					    this.afterExplodeFallProtect = true;
					}
				}
			} catch (Exception e) {
			    e.printStackTrace();
			    this.applyBlockMorph(Blocks.AIR.defaultBlockState(), new CompoundTag());
			}
		}
		//if (this.level().getBlockState(this.blockPosition()).getBlock() instanceof BaseFireBlock && !this.level().isClientSide) this.setTnt();
		if (this.getRemainingFireTicks() > 0 && !this.level().isClientSide) {
			this.setTnt();
		}
	}

	private void setFuse(int i) {
		CompoundTag progress = this.entityData.get(BRAKE_PROGRESS);
		progress = progress.copy();
		progress.putInt("fuse", i);
		this.entityData.set(BRAKE_PROGRESS, progress);
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	public void causeFallDamage(float f, float g, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
		if (this.isActive()) {
			cir.cancel();
			if (!(this.getBlockState().getBlock() instanceof AnvilBlock)) {
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

	public void applyBlockMorph(BlockState state, CompoundTag tag) {
		this.applyBlockMorph(state, tag, this.isMultiBlock());
	}

	public void applyBlockMorph(BlockState state, CompoundTag tag, boolean mb) {
		CompoundTag morphblocktag = this.entityData.get(DATA_BlockMorph);
		morphblocktag = morphblocktag.copy(); //FIXME:
		if (state.getBlock() instanceof EntityBlock bl) {
			CompoundTag blockEntityTag;
			try {
				BlockEntity ent = bl.newBlockEntity(this.blockPosition(), state);
				if (ent != null) {
					blockEntityTag = ent.saveWithoutMetadata();
				} else {
					blockEntityTag = new CompoundTag();
				}
				if (tag != null) {
					if (false)
						for (String key : tag.getAllKeys()) {
							if (blockEntityTag.contains(key)) {
								blockEntityTag.put(key, tag.get(key));
							}
						}
					blockEntityTag.merge(tag);
				}
			} catch (Exception e) {
				BlockomorphMod.LOGGER.warn("When receiving original tags from the block entity of the player " + this + " an error occurred: " + e.getMessage());
				blockEntityTag = new CompoundTag();
				blockEntityTag.merge(tag);
			}
			morphblocktag.put("Tags", blockEntityTag);
		} else {
			morphblocktag.remove("Tags");
		}
		morphblocktag.put("BlockState", NbtUtils.writeBlockState(state));
		morphblocktag.putBoolean("MultiBlock", mb);
		this.entityData.set(DATA_BlockMorph, morphblocktag, true);
		this.setFuse(-1);
		this.tnt = null;
		this.refreshDimensions();
	}

	public InteractionResult clickPlayer(Player clicker, BlockHitResult hiter, InteractionHand hand) {
		return this.clckTnt(clicker, hiter, hand);
	}

	private InteractionResult clckTnt(Player clicker, BlockHitResult hiter, InteractionHand hand) {
		ItemStack itemstack = clicker.getItemInHand(hand);
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
           return InteractionResult.PASS;
        } else {
           if (!clicker.level().isClientSide)this.setTnt();
           Item item = itemstack.getItem();
           if (!clicker.isCreative()) {
              if (itemstack.is(Items.FLINT_AND_STEEL)) {
                 itemstack.hurtAndBreak(1, clicker, (pl) -> {
                    pl.broadcastBreakEvent(hand);
                 });
              } else {
                 itemstack.shrink(1);
              }
           }

           clicker.awardStat(Stats.ITEM_USED.get(item));
           return InteractionResult.sidedSuccess(clicker.level().isClientSide);
        }
	}

	public void setTnt() {
		BlockState state = this.getBlockState();
		if (state.getBlock() instanceof TntBlock tnt && this.tnt == null) {
		    TntSpawnLevel lv = new TntSpawnLevel(this.level(), false, state);
		    PrimedTnt TNT;
		    try {
		    	tnt.onCaughtFire(state, lv, this.blockPosition(), null, (Player) (Object) this);
		    } catch (Exception e) {
		    	TNT = lv.extractTnt();
		    	if (TNT == null) {
		    		e.printStackTrace();
		    		return;
		    	}
		    }
			TNT = lv.extractTnt();
			if (TNT == null) {
				BlockPos ps = this.blockPosition();
				PrimedTnt primedtnt = new PrimedTnt(this.level(), (double)ps.getX() + 0.5D, (double)ps.getY(), (double)ps.getZ() + 0.5D, null);
                this.level().playSound((Player)null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level().gameEvent(null, GameEvent.PRIME_FUSE, ps);
                this.tnt = primedtnt;
			} else 
			    this.tnt = TNT;
			this.tnt.level();
			if (this.level().isClientSide()) {
				double d0 = this.level().random.nextDouble() * (double)((float)Math.PI * 2F);
                this.setDeltaMovement(new Vec3(-Math.sin(d0) * 0.02D, (double)0.2F, -Math.cos(d0) * 0.02D));
			}
		}
	}

	public PrimedTnt getTnt() {
		return this.tnt;
	}

	public BlockState getBlockState() {
		BlockState blockstate = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), this.entityData.get(DATA_BlockMorph).getCompound("BlockState"));
		return blockstate;
	}

	public CompoundTag getTag() {
		return this.entityData.get(DATA_BlockMorph).getCompound("Tags");
	}

	public boolean isActive() {
		return this.getBlockState().getBlock() != Blocks.AIR;
	}

	public boolean isFullActive() {
		return this.isActive() && this.tnt == null;
	}

	public void setReady(boolean flag) {
		this.readyForDestroy = flag;
	}

	public boolean readyForDestroy() {
		return this.readyForDestroy;
	}

	public HashMap<BlockPos, BlockState> getBlocks() {
		return this.blocks;
	}

	public boolean isMultiBlock() {
		return this.entityData.get(DATA_BlockMorph).getBoolean("MultiBlock");
	}

	public BlockPos minPos() {
		return this.minPos;
	}

	@Override
	public boolean canBeSeenByAnyone() {
		return !this.isActive();
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
	protected void getFallSounds(CallbackInfoReturnable<LivingEntity.Fallsounds> cir) {
		if (this.isActive())
			cir.setReturnValue(new LivingEntity.Fallsounds(SoundEvents.EMPTY, SoundEvents.EMPTY));
	}

	public void removePlayer(BlockPos pos, Player pl) {
		for (BlockBracker br : this.brackers) {
			if (br.getPos().equals(pos)) {
				br.removePlayer(pl);
				return;
			}
		}
	}

	private void updateBlocks() {
		this.blocks.clear();
		MultiBlockLevel lv = new MultiBlockLevel(this.level(), false);
		this.getBlockState().getBlock().setPlacedBy(lv, new BlockPos(0, 0, 0), this.getBlockState(), (LivingEntity) (Object) this, ItemStack.EMPTY);
		if (this.isMultiBlock())
			this.blocks = lv.getBlocks();
		CompoundTag tg = new CompoundTag();
		tg.putInt("fuse", -1);
		this.entityData.set(BRAKE_PROGRESS, tg);
		this.brackers.clear();
		this.brackers.add(new BlockBracker(this, new BlockPos(0, 0, 0), this.getBlockState(), this.entityData, BRAKE_PROGRESS));
		for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
			this.brackers.add(new BlockBracker(this, entry.getKey(), entry.getValue(), this.entityData, BRAKE_PROGRESS));
		}
	}

	public int getBiggestProgress() {
		int i = -1;
		try {
			for (BlockBracker br : this.brackers) {
				int j = br.getProgress();
				if (j > i)
					i = j;
			}
		} catch (Exception e) {
		}
		return i;
	}

	public void addPlayer(BlockPos pos, Player pl) {
		for (BlockBracker br : this.brackers) {
			if (br.getPos().equals(pos)) {
				br.addPlayer(pl);
				return;
			}
		}
	}

	public CompoundTag getProgress() {
		return this.entityData.get(BRAKE_PROGRESS);
	}

	@Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
	private void getStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
		if (this.isActive()) {
			cir.setReturnValue((this.maxPos.getY() - 1) + 0.83300006f);
			//cir.setReturnValue(1 + 0.83300006f);
		}
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (this.isActive()) {
			BlockPos minPos = this.minPos;
			BlockPos maxPos = this.maxPos;
			EntityDimensions dim = new EntityDimensions(0, 0, false) {
				public AABB makeBoundingBox(double d, double e, double f) {
					Vec3 vec3 = new Vec3(d, e, f);
					AABB ab = new AABB(vec3.x + minPos.getX(), vec3.y + minPos.getY(), vec3.z + minPos.getZ(), vec3.x + maxPos.getX(), vec3.y + maxPos.getY(), vec3.z + maxPos.getZ());
					return centerAABB(ab, vec3);
				}
			};
			cir.setReturnValue(dim);
		}
	}

	private AABB centerAABB(AABB original, Vec3 center) {
		double width = original.maxX - original.minX;
		double height = original.maxY - original.minY;
		double depth = original.maxZ - original.minZ;
		return new AABB(center.x - (width / 2), center.y, center.z - (depth / 2), center.x + (width / 2), center.y + height, center.z + (depth / 2));
	}

	private BlockPos findMinPos() {
		if (this.blocks.isEmpty())
			return new BlockPos(0, 0, 0);
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		HashMap<BlockPos, BlockState> blocks = new HashMap(this.blocks);
		blocks.put(new BlockPos(0, 0, 0), this.getBlockState());
		for (BlockPos pos : blocks.keySet()) {
			if (pos.getX() < minX) {
				minX = pos.getX();
			}
			if (pos.getY() < minY) {
				minY = pos.getY();
			}
			if (pos.getZ() < minZ) {
				minZ = pos.getZ();
			}
		}
		return new BlockPos(minX, minY, minZ);
	}

	private BlockPos findMaxPos() {
		if (this.blocks.isEmpty())
			return new BlockPos(1, 1, 1);
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		HashMap<BlockPos, BlockState> blocks = new HashMap(this.blocks);
		blocks.put(new BlockPos(0, 0, 0), this.getBlockState());
		for (BlockPos pos : blocks.keySet()) {
			if (pos.getX() > maxX) {
				maxX = pos.getX();
			}
			if (pos.getY() > maxY) {
				maxY = pos.getY();
			}
			if (pos.getZ() > maxZ) {
				maxZ = pos.getZ();
			}
		}
		return new BlockPos(maxX, maxY, maxZ).offset(1, 1, 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> p_20059_) {
		super.onSyncedDataUpdated(p_20059_);
		if (DATA_BlockMorph.equals(p_20059_)) {
			this.updateBlocks();
			this.minPos = this.findMinPos();
			this.maxPos = this.findMaxPos();
			this.refreshDimensions();
			if (this.level().isClientSide())
				this.clientUpdate();
		} else if (this.level().isClientSide() && BRAKE_PROGRESS.equals(p_20059_)) {
			int i = this.entityData.get(BRAKE_PROGRESS).getInt("fuse");
			if (i < 0) {
				this.tnt = null;
			} else {
				if (this.tnt == null) {
					this.setTnt();
				}
				if (this.tnt != null) {
					this.tnt.setFuse(i);
				}
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void clientUpdate() {
		if (Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc)
			sc.morphUpdate(this.getBlockState());
	}

	@Override
	@Nullable
	public ItemStack getPickResult() {
		if (this.isActive()) {
			Item item = this.getBlockState().getBlock().asItem();
			return new ItemStack(item);
		}
		return null;
	}

	public VoxelShape getShape() {
		VoxelShape shape = this.getBlockState().getCollisionShape(this.level(), this.blockPosition(), CollisionContext.of(this));
		for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
			VoxelShape shape2 = entry.getValue().getCollisionShape(this.level(), this.blockPosition(), CollisionContext.of(this));
			BlockPos pos = entry.getKey();
			shape = Shapes.or(shape, shape2.move(pos.getX(), pos.getY(), pos.getZ()));
		}
		return shape;
	}

	public VoxelShape getRenderShape(BlockPos pos) { //смещённый
		boolean flag = pos.equals(new BlockPos(0, 0, 0));
		if (this.blocks.containsKey(pos) || flag) {
			BlockState state;
			if (flag) {
				state = this.getBlockState();
			} else {
				state = this.blocks.get(pos);
			}
			VoxelShape shape = state.getShape(this.level(), this.blockPosition(), CollisionContext.of(this));
			VoxelShape shape2 = shape.move(pos.getX(), pos.getY(), pos.getZ());
			return shape2;
		}
		return null;
	}

	@Override
	public boolean isAttackable() {
		return !this.isActive();
	}

	@Override
	public boolean skipAttackInteraction(Entity ent) {
		return this.isActive();
	}

	@Override
	public void push(Entity mob) {
		if (!this.isActive())
			super.push(mob);
	}

	@Override
	protected void pushEntities() {
		if (!this.isActive())
			super.pushEntities();
	}
}
