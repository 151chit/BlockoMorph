package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.screens.BlockMorphConfigScreen;

import net.blockomorph.BlockomorphMod;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccessor {
   private static final EntityDataAccessor<CompoundTag> DATA_BlockMorph = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
   public List<Player> players = new ArrayList<>();
   private Player attacker; 
   private boolean braking;
   private boolean brake;
   private float progress;
   private float pie;
   private int animate = 0;
   private boolean readyForDestroy = true;

   public PlayerMixin(EntityType<? extends LivingEntity> type, Level world) {
      super(type, world);
   }

   @Inject(method = "defineSynchedData", at = @At("TAIL"), cancellable = true)
   protected void defineSynchedData(CallbackInfo ci) {
   	  this.entityData.define(DATA_PROGRESS, -1);
      CompoundTag morphblocktag = new CompoundTag();
      morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
      this.entityData.define(DATA_BlockMorph, morphblocktag);
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   public void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
    	if (tag.contains("BlockMorph")) {
    	    this.entityData.set(DATA_BlockMorph, tag.getCompound("BlockMorph"));
    	} else {
    		CompoundTag morphblocktag = new CompoundTag();
            morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
            this.entityData.set(DATA_BlockMorph, morphblocktag);
        }
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   public void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
      tag.put("BlockMorph", this.entityData.get(DATA_BlockMorph));
   }

   @Inject(method = "tick", at = @At("TAIL"), cancellable = true)
   public void tick(CallbackInfo ci) {
   	if (this.braking) {
      	if (this.brake) {
        this.progress += 1 / this.pie;
        while (this.progress >= 1.0f) {
        	this.entityData.set(DATA_PROGRESS, this.entityData.get(DATA_PROGRESS) + 1);
            if (this.entityData.get(DATA_PROGRESS) > 9) {
                MorphUtils.destroy(this, this.getAttacker());
            }
            this.progress -= 1;
        }
      	}
      	for (Iterator<Player> iterator = this.players.iterator(); iterator.hasNext();) {
             Player pl = iterator.next();
             if (MorphUtils.getEntityLookedAt(pl, -1) != this && ((PlayerAccessor)pl).readyForDestroy()) {
                 iterator.remove();
             } else if (!((PlayerAccessor)pl).readyForDestroy()) {
             	((PlayerAccessor)pl).setReady(true);
             }
        }
        float min = Float.MAX_VALUE;
        for (Player pl : this.players) {
        	float time = this.getTime(this.getBlockState(), pl.blockPosition(), pl);
        	if (time < min) {
        		min = time;
        		this.attacker = pl;
        	}
        	
        }
        this.setTimeFloat(min);
      	if (this.animate % 4 == 0 && this.level() instanceof ServerLevel lv) {
      		for (Player pla : this.players) {
      			pla.swing(InteractionHand.MAIN_HAND, true);
      		}
      		SoundType soundtype = this.getBlockState().getSoundType(this.level(), this.blockPosition(), null);
            this.level().playSound(null, this.blockPosition(), soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F);
      	}
      	this.animate++;
      	if (this.players.isEmpty()) {
      		this.stopDestroy();
      	}
      }
   }

   public void applyBlockMorph(BlockState state, CompoundTag tag) {
   	  CompoundTag morphblocktag = this.entityData.get(DATA_BlockMorph);
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
   	  this.entityData.set(DATA_BlockMorph, morphblocktag, true);
   	  this.refreshDimensions();
   	  
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

   public void setReady(boolean flag) {
   	    this.readyForDestroy = flag;
   }

   public boolean readyForDestroy() {
   	    return this.readyForDestroy;
   }

   public void stopDestroy() {
        this.braking = false;
      	this.entityData.set(DATA_PROGRESS, -1);
      	this.progress = 0;
      	this.pie = 0;
      	this.animate = 0;
      	this.brake = false;
   }

   public void removePlayer(Player pl) {
        if (this.players.contains(pl)) this.players.remove(pl);
   }

   @Nullable
   public Player getAttacker() {
   	    return this.attacker;
   }

   @Override
    public boolean canBeSeenByAnyone() {
    	return !this.isActive();
    }

   @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
   protected void getDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
      if (this.isActive()) cir.setReturnValue(null);
   }

   @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
   protected void getHurtSound(DamageSource damage, CallbackInfoReturnable<SoundEvent> cir) {
      if (this.isActive()) cir.setReturnValue(null);
   }

   @Inject(method = "getFallSounds", at = @At("HEAD"), cancellable = true)
   protected void getFallSounds(CallbackInfoReturnable<LivingEntity.Fallsounds> cir) {
      if (this.isActive()) cir.setReturnValue(new LivingEntity.Fallsounds(SoundEvents.EMPTY, SoundEvents.EMPTY));
   }

   public float getCoolDown(BlockState blockState, BlockPos blockPos, Player pl) {
        float progress = blockState.getDestroyProgress(pl, pl.level(), blockPos);
        if (Float.isInfinite(progress) || progress == 0) {
        	if (Float.isInfinite(progress)) MorphUtils.destroy(this, this.getAttacker());
        	return -1;
        }
        return (float)(0.1F / progress);
   }

   public float getTime(BlockState blockState, BlockPos blockPos, Player pl) {
   	    float cooldown = this.getCoolDown(blockState, blockPos, pl);
    	if (cooldown == -1) return cooldown;
    	float time = cooldown / 20;
    	return time;
   }

   public void setTimeFloat(float time2) {
   	    if (time2 <= 0) return;
   	    float time = time2 * 10;
    	this.pie = time * 20 / 10;
   }

   public boolean setTime(BlockState blockState, BlockPos blockPos, Player pl) {
    	float cooldown = this.getCoolDown(blockState, blockPos, pl);
    	if (cooldown == -1) return false;
    	float time = cooldown / 20;
    	time = time * 10;
    	this.pie = time * 20 / 10;
    	return true;
   }

   public void addPlayer(Player pl) {
    	if (!this.players.contains(pl)) {
    		((PlayerAccessor)pl).setReady(false);
    		this.players.add(pl);
    	}
    	if (!this.braking) {
    	this.entityData.set(DATA_PROGRESS, 0);
    	this.brake = this.setTime(this.getBlockState(), pl.blockPosition(), pl);
    	this.braking = true;
    	}
   }

   public int getProgress() {
    	return this.entityData.get(DATA_PROGRESS);
   }

   @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
   public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
      if (this.isActive()) {
      	cir.setReturnValue(new EntityDimensions(1f, 1f, true));
      }
   }

   @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
   private void getStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
      if(this.isActive()) {
         cir.setReturnValue(0.83300006f);
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_20059_) {
   	  super.onSyncedDataUpdated(p_20059_);
      if (DATA_BlockMorph.equals(p_20059_)) {
         this.refreshDimensions();
         if (this.level().isClientSide()) this.clientUpdate();
      } 
   }

   @OnlyIn(Dist.CLIENT)
   public void clientUpdate() {
   	  if (Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc) sc.morphUpdate(this.getBlockState());
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
   	  return shape;
   }

   public VoxelShape getRenderShape() {
   	  VoxelShape shape = this.getBlockState().getShape(this.level(), this.blockPosition(), CollisionContext.of(this));
   	  return shape;
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
      if (!this.isActive()) super.push(mob);
   }

   @Override
   protected void pushEntities() {
      if (!this.isActive()) super.pushEntities();
   }

}
