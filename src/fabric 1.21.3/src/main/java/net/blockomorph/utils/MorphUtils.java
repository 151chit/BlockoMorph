package net.blockomorph.utils;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.*;
import net.blockomorph.utils.*;
import net.blockomorph.utils.config.*;
import net.blockomorph.core.MainBus;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.TagKey;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.block.EntityBlock;

import com.mojang.blaze3d.platform.Window;

import java.util.Set;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.Blocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;
import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.ArrayList;

public class MorphUtils {
   public static final ResourceKey<DamageType> PLAYER_DESTROYED = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("blockomorph", "player_destroyed"));
   public static final ResourceKey<DamageType> PLAYER_DESTROYED_NULL = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("blockomorph", "player_destroyed_null"));
   private static boolean attackPressed;
   public static final SavedBlockManager bmanager = getSavedManager();
   public static Entity hitEntity;
   public static BlockPos hitPart;
   public static EntityHitResult hit;

   private static SavedBlockManager getSavedManager() {
   	    return new SavedBlockManager(FabricLoader.getInstance().getGameDir());
   }

   public static String isBannedBlock(BlockState state) {
   	    String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Config.Mode mode = (Config.Mode) Config.getInstance().getValue("listMode");
        if ((!state.isSolid() || state.getBlock() instanceof BarrierBlock || state.getBlock() instanceof MovingPistonBlock) && (state.getBlock() != net.minecraft.world.level.block.Blocks.AIR) && (boolean)Config.getInstance().getValue("solidBlocksOnly")) {
        	return "Block " + name + " not allowed because is solid!";
        } else if (mode == Config.Mode.WHITELIST) {
             if (!((List<String>)Config.getInstance().getValue("allowedBlocks")).contains(name)) 
                return "Block " + name + " not allowed because it not in whitelist!";
        } else if (mode == Config.Mode.BLACKLIST) {
             if (((List<String>)Config.getInstance().getValue("bannedBlocks")).contains(name)) 
                return "Block " + name + " not allowed because it in blacklist!";
        }
        return "";
   }

   public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        CompoundTag originalNBT = oldPlayer.saveWithoutId(new CompoundTag());
        CompoundTag newNBT = newPlayer.saveWithoutId(new CompoundTag());

        if (originalNBT.contains("BlockMorph")) {
            CompoundTag tag = new CompoundTag();
            tag.put("BlockMorph", originalNBT.getCompound("BlockMorph"));
            newPlayer.load(tag);
            newPlayer.refreshDimensions();
        }
   }

   public static VoxelShape centerVoxelShape(VoxelShape vo, PlayerAccessor pl) {
        BlockPos minpos = pl.minPos();
        Player player = (Player)pl;
        AABB hitbox = player.getBoundingBox();
        Vec3 playerCenter = player.position();
        double hitboxMinX = hitbox.minX;
        double hitboxMinZ = hitbox.minZ;

        double offsetX = hitbox.minX - (playerCenter.x + minpos.getX());
        double offsetZ = hitbox.minZ - (playerCenter.z + minpos.getZ());

        return vo.move(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
   }

   public static boolean onPlayerAttack(Player player, Entity mob, BlockPos part) {
   	    if (mob.level().isClientSide() && mob instanceof PlayerAccessor mb2) mb2.setReady(false);
   	    if (mob instanceof PlayerAccessor mb && player instanceof ServerPlayer pl && mb.isActive() && part != null) {
   	    	GameType gm = pl.gameMode.getGameModeForPlayer();
    	    if (player.isCreative()) {
    	    	destroy(mb, player);
    	    	player.swing(InteractionHand.MAIN_HAND, true);
    	    } else if (gm == GameType.SURVIVAL) {
    	    	mb.addPlayer(part, player);
    	    }
    	    return true;	    
    	}
    	return false;
   }

   public static boolean onPlayerAttacked(LivingEntity attacked, DamageSource damage, float amount) {
   	    if (attacked instanceof PlayerAccessor pl) {
   	        boolean allowed =
   	        damage.is(DamageTypes.GENERIC_KILL) || 
   	        damage.is(DamageTypes.FELL_OUT_OF_WORLD) ||
   	        damage.is(DamageTypes.PLAYER_EXPLOSION) ||
   	        damage.is(DamageTypes.EXPLOSION);
   	        if (pl.isActive()) {
   	        	if (allowed) {
   	        		destroy(pl, damage.getEntity());
   	        	}
   	        	if (!(damage.is(PLAYER_DESTROYED) || damage.is(PLAYER_DESTROYED_NULL))) return true; 
   	        }
   	    }
   	    return false;
   }

   @Environment(EnvType.CLIENT)
   public static void onClientTick() {
   	Minecraft mc = Minecraft.getInstance();
   	        if (mc.player != null) {
                boolean isAttackPressed = mc.options.keyAttack.isDown();

                if (hitEntity instanceof PlayerAccessor pl && pl.isActive()) {
                	int i = pl.getBiggestProgress();
                	if ((hitPart != null && !isAttackPressed && i > -1) || (attackPressed && !isAttackPressed)) {
                        ClientPlayNetworking.send(new ServerBoundInteractBlockPacket(false, -1, hitPart));
                        pl.setReady(true);
                	}
                    if (i > -1 && hit != null && isAttackPressed) {
                    	//crackBlock(pl, calculateHitDirection(hit.getLocation(), hitEntity.getBoundingBox()));
                    	crackBlock(pl, getClosestHitSide(pl.getRenderShape(hitPart).move(hitEntity.position()).move(-0.5, 0, -0.5), hit.getLocation()), hitPart);
                    }
                    if (isAttackPressed && pl.readyForDestroy()) {
                    	GamemodeAccessor gm = ((GamemodeAccessor)mc.gameMode);
                    	if (gm.getDelay() > 0) {
                    		gm.setDelay(gm.getDelay() - 1);
                    	} else {
                    		ClientPlayNetworking.send(new ServerBoundInteractBlockPacket(true, hitEntity.getId(), hitPart));
                    		onPlayerAttack(mc.player, hitEntity, null);
                    	}
                    }
                }
                attackPressed = isAttackPressed;

            }
   }

   public static AbstractMap.SimpleEntry<BlockPos, EntityHitResult> getPlayerLookedResult(Player player, double distance, float timeCalapse) {
   	    if (distance < 0) distance = player.blockInteractionRange();
   	    
        Vec3 eyePosition = player.getEyePosition(timeCalapse);
        Vec3 lookVector = player.getViewVector(timeCalapse);
        Vec3 reachVector = eyePosition.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);

        Vec3 blockhit = player.level().clip(new ClipContext(eyePosition, reachVector, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getLocation();
        
        AABB aabb = new AABB(eyePosition, reachVector).inflate(1.0D);
        List<Entity> entities = player.level().getEntities(player, aabb);

        Entity closestEntity = null;
        double closestDistance = distance;
        Optional<Vec3> result = Optional.empty();
        EntityHitResult hit2 = null;
        BlockPos hitP = null;

        List<Hit> hits = new ArrayList<>();

        for (Entity entity : entities) {
        	if (entity instanceof PlayerAccessor mob) {
        	  HashMap<BlockPos, BlockState> blocks = new HashMap(mob.getBlocks());
        	  blocks.put(new BlockPos(0, 0, 0), mob.getBlockState());
        	  for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                VoxelShape voxelShape = entry.getValue().getShape(entity.level(), entity.blockPosition(), CollisionContext.of(entity));
                BlockPos pos = entry.getKey();
                voxelShape = voxelShape.move(pos.getX(), pos.getY(), pos.getZ());
                //voxelShape = voxelShape.move(entity.getX() - 0.5, entity.getY(), entity.getZ() - 0.5);
                voxelShape = centerVoxelShape(voxelShape, mob);

                for (AABB entityAABB : voxelShape.toAabbs()) {
                     result = entityAABB.clip(eyePosition, reachVector);

                     if (result.isPresent()) {
                         double entityDistance = eyePosition.distanceTo(result.get());

                         if (entityDistance < closestDistance) {
                             closestDistance = entityDistance;
                             closestEntity = entity;
                             hit2 = new EntityHitResult(closestEntity, result.get());
                             hitP = pos;
                             hits.add(new Hit(closestEntity, closestDistance, result, hit2, hitP));
                         }
                     }
                }
                closestEntity = null;
                closestDistance = distance;
                result = Optional.empty();
                hit2 = null;
                hitP = null;
        	  }
        	}
        }
        hits.sort(Comparator.comparingDouble(hit -> eyePosition.distanceTo(hit.result().get())));
        if (!hits.isEmpty()) {
        	Hit hit = hits.get(0);
        	if (blockhit != null && hit.result().isPresent()) {
        	    if (hit.closestEntity() != null && hit.closestDistance() > eyePosition.distanceTo(blockhit)) return new AbstractMap.SimpleEntry(null, null);
            }
            return new AbstractMap.SimpleEntry(hit.hitP(), hit.hit2());
        }

        return new AbstractMap.SimpleEntry(null, null);
   }

   @Environment(EnvType.CLIENT)
   public static boolean onAttackBlockPlayer() { 
   	    Minecraft mc = Minecraft.getInstance();
   	    if (hit != null && hit.getEntity() instanceof PlayerAccessor pl && pl.isActive()) {
   	    	if (hitEntity instanceof Player) {
   	    	    ClientPlayNetworking.send(new ServerBoundInteractBlockPacket(true, hitEntity.getId(), hitPart));
   	    	    onPlayerAttack(mc.player, hitEntity, null);
   	    	}
   	    	return true;
   	    }
   	    return false;
   }

   @Environment(EnvType.CLIENT)
   public static void onPick() { 
   	 Minecraft mc = Minecraft.getInstance();
   	 boolean isAttackPressed = mc.options.keyAttack.isDown();
   	 if (mc.getCameraEntity() instanceof Player pl) {
   	 	AbstractMap.SimpleEntry<BlockPos, EntityHitResult> hitter = getPlayerLookedResult(pl, -1, 1);
   	 	hitPart = hitter.getKey();
   	 	hit = hitter.getValue();
   	 	Entity ent;
   	 	if (hit == null) {
   	 		ent = null;
   	 	} else ent = hit.getEntity();
   	 	if (ent != hitEntity && hitEntity != null) ((PlayerAccessor)hitEntity).setReady(true);
   	 	if (hitEntity != null && !hitEntity.isAlive() && isAttackPressed) ((GamemodeAccessor)mc.gameMode).setDelay(5);
   	    hitEntity = ent;
   	 }
   }

   @Environment(EnvType.CLIENT)
   public static boolean onHudRender(GuiGraphics GUI) {
   	    Minecraft mc = Minecraft.getInstance();
   	    Entity player = mc.getCameraEntity();
   	    
   	    if (player instanceof PlayerAccessor pl && pl.isActive()) {
   	        int width = mc.getWindow().getGuiScaledWidth();
		    int height = mc.getWindow().getGuiScaledHeight();
            renderBlockHeart(GUI, (Player)player, pl, width, height);
            return true;
        }
        return false;
   }

   @Environment(EnvType.CLIENT)
   private static void renderBlockHeart(GuiGraphics gui, Player player, PlayerAccessor pl, int width, int height) {
       int maxHearts = 10;
       int progress = pl.getBiggestProgress();

       BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
       BakedModel model = dispatcher.getBlockModel(pl.getBlockState());
       TextureAtlasSprite sprite = model.getParticleIcon();


       int x = width / 2 - 91;
       int y = height - 39;

       renderBar(gui, x, y, progress);
       
       for (int i = 0; i < maxHearts; i++) {
        int xPos = x + i * 8;
        int yPos = y;


        if (i < 9 - progress) {
            gui.blitSprite(RenderType::guiTextured, sprite, xPos + 1, yPos + 1, 7, 7);
        }
       }
   }

   @Environment(EnvType.CLIENT)
   private static void renderBar(GuiGraphics graphics, int x, int y, int progress) {
        if (progress == 9) {
        	graphics.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/icons.png"), x, y, 0, 10, 81, 9, 81, 19);
        } else {
        	graphics.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/icons.png"), x, y, 0, 0, 81, 9, 81, 19);
        }    
   }

   public static void pickBlockPlayer(Player pl, ItemStack itemstack) {
   	        MultiPlayerGameMode gm = Minecraft.getInstance().gameMode;
   	        Inventory inventory = pl.getInventory();
   	        boolean flag = pl.getAbilities().instabuild;

            int i = inventory.findSlotMatchingItem(itemstack);
            if (flag) {
               inventory.setPickedItem(itemstack);
               gm.handleCreativeModeItemAdd(pl.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
            } else if (i != -1) {
               if (Inventory.isHotbarSlot(i)) {
                  inventory.selected = i;
               } else {
                  gm.handlePickItem(i);
               }
            }
   }

   public static void destroy(PlayerAccessor mob_pl, @Nullable Entity attacker) {
    	Entity mob = (Player)mob_pl;
    	HashMap<BlockPos, BlockState> blocks = new HashMap(mob_pl.getBlocks());
    	blocks.put(new BlockPos(0, 0, 0), mob_pl.getBlockState());
        
        Holder<DamageType> damage = mob.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).
        getOrThrow(attacker == null ? PLAYER_DESTROYED_NULL : PLAYER_DESTROYED);
    	
    	if (Config.getInstance().getValue("playerDieAfterDestroy")) {
    		mob.hurt(new DamageSource(damage, attacker), Float.MAX_VALUE);
    	} else {
    		mob_pl.applyBlockMorph(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), new CompoundTag(), false);
    	}

    	if (mob.level() instanceof ServerLevel lv) {
        	for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            	BlockPos pos = entry.getKey();
            	BlockState val = entry.getValue();
            	VoxelShape shape2 = entry.getValue().getCollisionShape(lv, mob.blockPosition(), CollisionContext.of(mob));
            	particle(lv, mob.getX() + pos.getX(), mob.getY() + pos.getY(), mob.getZ() + pos.getZ(), val, shape2);
            	SoundType soundtype = val.getSoundType();
                lv.playSound(null, mob.blockPosition().offset(pos), soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            }
    	}
   }

   public static Entity getEntityLookedAt(Player player, double distance, float c) {
   	    EntityHitResult hit = getPlayerLookedResult(player, distance, c).getValue();
   	    if (hit != null) {
   	    	return hit.getEntity();
   	    }
   	    return null;
   }

   public static BlockPos getEntityPartLookedAt(Player player, double distance, float c) {
   	    return getPlayerLookedResult(player, distance, c).getKey();
   }

   private static Direction calculateHitDirection(Vec3 hitVec, AABB boundingBox) {
    double xDist = Math.min(Math.abs(hitVec.x - boundingBox.minX), Math.abs(hitVec.x - boundingBox.maxX));
    double yDist = Math.min(Math.abs(hitVec.y - boundingBox.minY), Math.abs(hitVec.y - boundingBox.maxY));
    double zDist = Math.min(Math.abs(hitVec.z - boundingBox.minZ), Math.abs(hitVec.z - boundingBox.maxZ));

    if (xDist < yDist && xDist < zDist) {
        return hitVec.x < boundingBox.getCenter().x ? Direction.WEST : Direction.EAST;
    } else if (yDist < xDist && yDist < zDist) {
        return hitVec.y < boundingBox.getCenter().y ? Direction.DOWN : Direction.UP;
    } else {
        return hitVec.z < boundingBox.getCenter().z ? Direction.NORTH : Direction.SOUTH;
    }
   }

   @Nullable
   public static Direction getClosestHitSide(VoxelShape voxelShape, Vec3 hitPosition) {
        for (AABB aabb : voxelShape.toAabbs()) {
            if (containsAABB(aabb, hitPosition, 1.0E-7)) {
            	return calculateHitDirection(hitPosition, aabb);
            }
        }

        return null;
   }

   private static boolean containsAABB(AABB ab, Vec3 tr, double tolerance) {
   	    double d = tr.x;
   	    double e = tr.y;
   	    double f = tr.z;
        return (d >= ab.minX - tolerance && d <= ab.maxX + tolerance) &&
           (e >= ab.minY - tolerance && e <= ab.maxY + tolerance) &&
           (f >= ab.minZ - tolerance && f <= ab.maxZ + tolerance);
   }

   private static void particle(ServerLevel world, double x, double y, double z, BlockState blockState, VoxelShape shape) {
    if (!blockState.isAir() && blockState.shouldSpawnTerrainParticles()) {
        VoxelShape voxelShape = shape;
        double d0 = 0.25D;
        
        voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double d1 = Math.min(1.0D, maxX - minX);
            double d2 = Math.min(1.0D, maxY - minY);
            double d3 = Math.min(1.0D, maxZ - minZ);
            int i = Math.max(2, Mth.ceil(d1 / 0.25D));
            int j = Math.max(2, Mth.ceil(d2 / 0.25D));
            int k = Math.max(2, Mth.ceil(d3 / 0.25D));

            for (int l = 0; l < i; ++l) {
                for (int i1 = 0; i1 < j; ++i1) {
                    for (int j1 = 0; j1 < k; ++j1) {
                        double d4 = ((double) l + 0.5D) / (double) i;
                        double d5 = ((double) i1 + 0.5D) / (double) j;
                        double d6 = ((double) j1 + 0.5D) / (double) k;
                        double particleX = d4 * d1;
                        double particleY = d5 * d2;
                        double particleZ = d6 * d3;

                        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
                        world.sendParticles(
                            particleData,
                            x + particleX - 0.5D, y + particleY, z + particleZ - 0.5D,
                            1, 
                            d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, 
                            0.25D 
                        );
                    }
                }
            }
        });
    }
   }

   @Environment(EnvType.CLIENT)
   public static void crackBlock(PlayerAccessor player, Direction dir, BlockPos part) {
      Entity pl = (Player)player;
      Level level = pl.level();
      BlockState blockstate;
      if (part.equals(BlockPos.ZERO)) {
          blockstate = player.getBlockState();
      } else {
          blockstate = player.getBlocks().get(part);
          if (blockstate == null) {
              blockstate = player.getBlockState();
          }
      }
      if (blockstate.getRenderShape() != RenderShape.INVISIBLE && level instanceof ClientLevel lv && blockstate.shouldSpawnTerrainParticles()) {
         double i = pl.getX() + part.getX();
         double j = pl.getY() + part.getY();
         double k = pl.getZ() + part.getZ();
         float f = 0.1F;
         AABB aabb = blockstate.getShape(level, pl.blockPosition().offset(part)).bounds();
         aabb = aabb.move(-0.5, 0, -0.5);
         RandomSource random = RandomSource.create();
         double d0 = i + random.nextDouble() * (aabb.maxX - aabb.minX - (double)0.2F) + f + aabb.minX;
         double d1 = j + random.nextDouble() * (aabb.maxY - aabb.minY - (double)0.2F) + f + aabb.minY;
         double d2 = k + random.nextDouble() * (aabb.maxZ - aabb.minZ - (double)0.2F) + f + aabb.minZ;
         if (dir == Direction.DOWN) {
            d1 = j + aabb.minY - f;
         }

         if (dir == Direction.UP) {
            d1 = j + aabb.maxY + f;
         }

         if (dir == Direction.NORTH) {
            d2 = k + aabb.minZ - f;
         }

         if (dir == Direction.SOUTH) {
            d2 = k + aabb.maxZ + f;
         }

         if (dir == Direction.WEST) {
            d0 = i + aabb.minX - f;
         }

         if (dir == Direction.EAST) {
            d0 = i + aabb.maxX + f;
         }

         Minecraft.getInstance().particleEngine.add((new TerrainParticle(lv, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate)).setPower(0.2F).scale(0.6F));
      }
   }

   private static record Hit(
   	    Entity closestEntity,
        double closestDistance,
        Optional<Vec3> result,
        EntityHitResult hit2,
        BlockPos hitP
   ) {}
}
