package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.blockomorph.utils.*;
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
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.AnvilBlock;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.util.Mth;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerAccessor {
   private static final EntityDataAccessor<CompoundTag> DATA_BlockMorph = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   private static final EntityDataAccessor<CompoundTag> BRAKE_PROGRESS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   private final List<BlockBracker> brackers = new CopyOnWriteArrayList();
   private HashMap<BlockPos, BlockState> blocks = new HashMap();
   private boolean readyForDestroy = true;
   private BlockPos minPos = new BlockPos(0, 0, 0);
   private BlockPos maxPos = new BlockPos(0, 0, 0);

   public PlayerMixin(EntityType<? extends LivingEntity> type, Level world) {
      super(type, world);
   }

   @Inject(method = "defineSynchedData", at = @At("TAIL"))
   protected void defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
   	  CompoundTag morphblocktag = new CompoundTag();
      morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
      morphblocktag.putBoolean("MultiBlock", false);
      builder.define(DATA_BlockMorph, morphblocktag);
      builder.define(BRAKE_PROGRESS, new CompoundTag());
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   public void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
    	if (tag.contains("BlockMorph")) {
    	    this.entityData.set(DATA_BlockMorph, tag.getCompound("BlockMorph"), true); //FIXME:
    	} else {
    		CompoundTag morphblocktag = new CompoundTag();
            morphblocktag.put("BlockState", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
            morphblocktag.putBoolean("MultiBlock", false);
            this.entityData.set(DATA_BlockMorph, morphblocktag, true); // FIXME:
        }
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   public void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
      tag.put("BlockMorph", this.entityData.get(DATA_BlockMorph));
   }

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   public void attack(Entity entity, CallbackInfo ci) {
   	    if (entity instanceof PlayerAccessor pl && pl.isActive()) ci.cancel();
   }

   @Inject(method = "tick", at = @At("TAIL"), cancellable = true)
   public void tick(CallbackInfo ci) {
   	   for (Iterator<BlockBracker> iterator = this.brackers.iterator(); iterator.hasNext();) {
   	   	   iterator.next().tick();
   	   }
   	   //SoundType soundtype = this.getBlockState().getSoundType();
   	   //if (this.level() instanceof ServerLevel lv && this.isInWater())
       //lv.playSound(null, this.blockPosition(), soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
   }

   public boolean isOnFire() {
   	   if (this.isActive()) return false;
   	   return super.isOnFire();
   }
   
   public boolean hasEffect(Holder<MobEffect> holder) {
   	    if (this.isActive() && holder.equals(MobEffects.WATER_BREATHING)) return true;
        return super.hasEffect(holder);
   }

   public int getAirSupply() {
   	   if (this.isActive()) {
   	   	  if (this.isEyeInFluid(FluidTags.WATER)) {
   	   	  	return 0;
   	   	  }
   	   }
   	   return super.getAirSupply();
   }

   public float getHealth() {
   	   if (super.getHealth() == 0) return 0;
   	   if (this.isActive()) return 20;
       return super.getHealth();
   }

   public void setAirSupply(int i) {
   	   if (this.isActive()) i = 0;
       super.setAirSupply(i);
   }

   public double getAttributeValue(Holder<Attribute> attribute) {
   	   if (this.isActive()) {
   	   	   if (attribute.equals(Attributes.MAX_HEALTH)) return 20;
   	   }
       return super.getAttributeValue(attribute);
   }

   @Inject(method = "getAbsorptionAmount", at = @At("HEAD"), cancellable = true)
   public void getAbsorptionAmount(CallbackInfoReturnable<Float> cir) {
       if (this.isActive()) cir.setReturnValue(0f);
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
        float h = Math.min(Mth.floor((float)i * 2), 40);
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
   	          blockEntityTag = ent.saveWithoutMetadata(this.level().registryAccess());
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

   public HashMap<BlockPos, BlockState> getBlocks() {
   	    return this.blocks;
   }

   public boolean isMultiBlock() {
   	    return this.entityData.get(DATA_BlockMorph).getBoolean("MultiBlock");
   }

   public BlockPos minPos() {
   	    return this.minPos;
   }

   public void setReady(boolean flag) {
   	    this.readyForDestroy = flag;
   }

   public boolean readyForDestroy() {
   	    return this.readyForDestroy;
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
		MultiBlockLevel lv = new MultiBlockLevel(this.level());
		this.getBlockState().getBlock().setPlacedBy(lv, new BlockPos(0, 0, 0), this.getBlockState(), (LivingEntity)(Object)this, ItemStack.EMPTY);
		if (this.isMultiBlock()) this.blocks = lv.getBlocks();
		this.entityData.set(BRAKE_PROGRESS, new CompoundTag());
		this.brackers.clear();
		this.brackers.add(new BlockBracker(
			this,
			new BlockPos(0, 0, 0),
			this.getBlockState(),
			this.entityData,
			BRAKE_PROGRESS
		));
		for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
			this.brackers.add(new BlockBracker(
			    this,
			    entry.getKey(),
			    entry.getValue(),
			    this.entityData,
			    BRAKE_PROGRESS
		    ));
		}
   }

   public int getBiggestProgress() {
   	    int i = -1;
   	    try {
   	        for (BlockBracker br : this.brackers) {
   	        	int j = br.getProgress();
   	   	        if (j > i) i = j;
   	        }
   	    } catch (Exception e) {}
   	    return i;
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

   @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
   public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
      if (this.isActive()) {
      	BlockPos minPos = this.minPos;
        BlockPos maxPos = this.maxPos;
        EntityDimensions dim = EntityDimensions.fixed(1f, 1f).withEyeHeight((maxPos.getY() - 1) + 0.83300006f);
      	((DimAccessor)(Object)dim).setListener((vec3) -> {
      		AABB ab = new AABB(
                vec3.x + minPos.getX(), 
                vec3.y + minPos.getY(),
                vec3.z + minPos.getZ(),
                vec3.x + maxPos.getX(),
                vec3.y + maxPos.getY(),
                vec3.z + maxPos.getZ()
            );
            return this.centerAABB(ab, vec3);
      	});
      	cir.setReturnValue(dim);
      }
   }

   private AABB centerAABB(AABB original, Vec3 center) {
      double width = original.maxX - original.minX;
      double height = original.maxY - original.minY;
      double depth = original.maxZ - original.minZ;

      return new AABB(
        center.x - (width / 2),
        center.y,
        center.z - (depth / 2),
        center.x + (width / 2),
        center.y + height,
        center.z + (depth / 2)
      );
   }

   private BlockPos findMinPos() {
   	  if (this.blocks.isEmpty()) return new BlockPos(0, 0, 0);
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
   	  if (this.blocks.isEmpty()) return new BlockPos(1, 1, 1);
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
         if (this.level().isClientSide()) this.clientUpdate();
      } 
   }

   @Environment(EnvType.CLIENT)
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
      if (!this.isActive()) super.push(mob);
   }

   @Override
   protected void pushEntities() {
      if (!this.isActive()) super.pushEntities();
   }

}
