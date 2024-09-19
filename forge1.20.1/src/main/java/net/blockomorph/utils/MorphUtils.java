package net.blockomorph.utils;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.ServerBoundStopDestroyPacket;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.GamemodeAccessor;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.entity.living.LivingDrownEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

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
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class MorphUtils {
   public static final ResourceKey<DamageType> PLAYER_DESTROYED = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("blockomorph:player_destroyed"));
   public static final ResourceKey<DamageType> PLAYER_DESTROYED_NULL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("blockomorph:player_destroyed_null"));
   private static boolean attackPressed;
   public static Entity hitEntity;
   public static EntityHitResult hit;

   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalNBT = event.getOriginal().saveWithoutId(new CompoundTag());
        CompoundTag newNBT = event.getEntity().saveWithoutId(new CompoundTag());

        if (originalNBT.contains("BlockMorph")) {
            CompoundTag tag = new CompoundTag();
            tag.put("BlockMorph", originalNBT.getCompound("BlockMorph"));
            event.getEntity().load(tag);
            event.getEntity().refreshDimensions();
        }
   }

   @SubscribeEvent
   public static void onPlayerAttack(AttackEntityEvent event) {
   	    Player player = event.getEntity();
   	    Entity mob = event.getTarget();
   	    if (mob.level().isClientSide() && mob instanceof PlayerAccessor mb2) mb2.setReady(false);
   	    if (mob instanceof PlayerAccessor mb && player instanceof ServerPlayer pl && mb.isActive()) {
   	    	event.setCanceled(true);
   	    	GameType gm = pl.gameMode.getGameModeForPlayer();
    	    if (player.isCreative()) {
    	    	destroy(mb, player);
    	    	player.swing(InteractionHand.MAIN_HAND, true);
    	    } else if (gm == GameType.SURVIVAL) {
    	    	mb.addPlayer(player);
    	    } else if (gm == GameType.ADVENTURE) {
    	    	
    	    }
    	    
    	}
   }

   @SubscribeEvent
   public static void onPlayerAttacked(LivingAttackEvent event) {
   	    Entity attacked = event.getEntity();
   	    DamageSource damage = event.getSource();
   	    if (attacked instanceof PlayerAccessor pl) {
   	        boolean allowed =
   	        damage.is(DamageTypes.GENERIC_KILL) || 
   	        damage.is(DamageTypes.FELL_OUT_OF_WORLD) ||
   	        damage.is(DamageTypes.PLAYER_EXPLOSION) ||
   	        damage.is(DamageTypes.EXPLOSION);
   	        if (pl.isActive()) {
   	        	if (!(damage.is(PLAYER_DESTROYED) || damage.is(PLAYER_DESTROYED_NULL))) event.setCanceled(true);
   	        	if (allowed) {
   	        		destroy(pl, damage.getEntity());
   	        	}
   	        }
   	    }
   }

   @SubscribeEvent
   public static void onPlayerDrown(LivingDrownEvent event) {
   	    if (event.getEntity() instanceof PlayerAccessor pl) {
   	    	if (pl.isActive()) event.setDrowning(false);
   	    }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
   	Minecraft mc = Minecraft.getInstance();
   	        if (mc.player != null && event.phase == TickEvent.Phase.END) {
                boolean isAttackPressed = mc.options.keyAttack.isDown();

                if (hitEntity instanceof PlayerAccessor pl && pl.isActive()) {
                	if ((!isAttackPressed && pl.getProgress() > -1) || (attackPressed && !isAttackPressed)) {
                        BlockomorphMod.PACKET_HANDLER.sendToServer(new ServerBoundStopDestroyPacket());
                        pl.setReady(true);
                	}
                    HitResult hito = hit;
                    if (pl.getProgress() > -1 && hito != null && isAttackPressed) {
                    	crackBlock(pl, calculateHitDirection(hito.getLocation(), hitEntity.getBoundingBox()));
                    }
                    if (isAttackPressed && pl.readyForDestroy()) {
                    	GamemodeAccessor gm = ((GamemodeAccessor)mc.gameMode);
                    	if (gm.getDelay() > 0) {
                    		gm.setDelay(gm.getDelay() - 1);
                    	} else {
                    		mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(hitEntity, mc.player.isShiftKeyDown()));
   	    	                if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
                                mc.player.attack(hitEntity);
                            }
                    	}
                    }
                }
                attackPressed = isAttackPressed;

            }
   }

   public static EntityHitResult getPlayerLookedResult(Player player, double distance) {
   	    if (distance < 0) distance = player.getBlockReach();
   	    
        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 reachVector = eyePosition.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);

        Vec3 blockhit = player.level().clip(new ClipContext(eyePosition, reachVector, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getLocation();
        
        AABB aabb = new AABB(eyePosition, reachVector).inflate(1.0D);
        List<Entity> entities = player.level().getEntities(player, aabb);

        Entity closestEntity = null;
        double closestDistance = distance;
        Optional<Vec3> result = Optional.empty();
        EntityHitResult hit2 = null;

        for (Entity entity : entities) {
        	if (entity instanceof PlayerAccessor mob) {
                VoxelShape voxelShape = mob.getRenderShape();
                voxelShape = voxelShape.move(entity.getX() - 0.5, entity.getY(), entity.getZ() - 0.5);

                for (AABB entityAABB : voxelShape.toAabbs()) {
                     result = entityAABB.clip(eyePosition, reachVector);

                     if (result.isPresent()) {
                         double entityDistance = eyePosition.distanceTo(result.get());

                         if (entityDistance < closestDistance) {
                             closestDistance = entityDistance;
                             closestEntity = entity;
                             hit2 = new EntityHitResult(closestEntity, result.get());
                             break; 
                         }
                     }
                }
        	}
        }

        if (blockhit != null && result.isPresent()) {
        	if (closestEntity != null && closestDistance > eyePosition.distanceTo(blockhit)) return null;
        }

        return hit2;
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onRenderFire(RenderBlockScreenEffectEvent event) {
   	if (((PlayerAccessor)event.getPlayer()).isActive()) {
   		if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE) event.setCanceled(true);
   	}
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onAttackBlockPlayer(InputEvent.InteractionKeyMappingTriggered event) {
   	    Minecraft mc = Minecraft.getInstance();
   	    if (hit != null && hit.getEntity() instanceof PlayerAccessor pl && pl.isActive() && event.isAttack()) {
   	    	event.setCanceled(true);
   	    	if (hitEntity instanceof Player) {
   	    	    mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(hitEntity, mc.player.isShiftKeyDown()));
   	    	    if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
                    mc.player.attack(hitEntity);
                }
   	    	}
   	    }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onPick(ViewportEvent.ComputeCameraAngles event) {
   	 Minecraft mc = Minecraft.getInstance();
   	 boolean isAttackPressed = mc.options.keyAttack.isDown();
   	 if (mc.getCameraEntity() instanceof Player pl) {
   	 	Entity ent = getEntityLookedAt(pl, -1);
   	 	if (ent != hitEntity && hitEntity != null) ((PlayerAccessor)hitEntity).setReady(true);
   	 	if (hitEntity != null && !hitEntity.isAlive() && isAttackPressed) ((GamemodeAccessor)mc.gameMode).setDelay(5);
   	    hitEntity = ent;
   	    hit = getPlayerLookedResult(pl, -1);
   	 }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onHudRender(RenderGuiOverlayEvent.Pre event) {
   	    Minecraft mc = Minecraft.getInstance();
   	    Entity player = mc.getCameraEntity();
   	    ResourceLocation overlayId = event.getOverlay().id();
   	    boolean flag = mc.gameMode.canHurtPlayer();
   	    
   	    if (player instanceof PlayerAccessor pl && pl.isActive()) {
   	        if (flag && overlayId.equals(new ResourceLocation("minecraft", "player_health"))) {
   	           Window w = event.getWindow();
   	           int width = w.getGuiScaledWidth();
               int height = w.getGuiScaledHeight();
               renderBlockHeart(event.getGuiGraphics(), (Player)player, pl, width, height);
               ((ForgeGui)mc.gui).leftHeight += 10;
               event.setCanceled(true);
   	        }
   	        if (overlayId.equals(new ResourceLocation("minecraft", "air_level")))
               event.setCanceled(true);
        }
   }

   @OnlyIn(Dist.CLIENT)
   private static void renderBlockHeart(GuiGraphics gui, Player player, PlayerAccessor pl, int width, int height) {
       int maxHearts = 10;
       int progress = pl.getProgress();

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
            gui.blit(xPos + 1, yPos + 1, 0, 7, 7, sprite);
        }
       }
   }

   @OnlyIn(Dist.CLIENT)
   private static void renderBar(GuiGraphics graphics, int x, int y, int progress) {
        if (progress == 9) {
        	graphics.blit(new ResourceLocation("blockomorph:textures/screens/icons.png"), x, y, 0, 10, 81, 9, 81, 19);
        } else {
        	graphics.blit(new ResourceLocation("blockomorph:textures/screens/icons.png"), x, y, 0, 0, 81, 9, 81, 19);
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

        Collection<TagKey<DamageType>> type = Set.of(
        	DamageTypeTags.BYPASSES_INVULNERABILITY,
        	DamageTypeTags.BYPASSES_RESISTANCE,
        	DamageTypeTags.BYPASSES_EFFECTS,
        	DamageTypeTags.BYPASSES_COOLDOWN,
        	DamageTypeTags.BYPASSES_ENCHANTMENTS,
        	DamageTypeTags.BYPASSES_SHIELD,
        	DamageTypeTags.BYPASSES_ARMOR
        );
        
        Holder<DamageType> damage = mob.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).
        getHolderOrThrow(attacker == null ? PLAYER_DESTROYED_NULL : PLAYER_DESTROYED);
        ((Holder.Reference<DamageType>) damage).bindTags(type);
    	
    	mob.hurt(new DamageSource(damage, attacker), Float.MAX_VALUE);

    	if (mob.level() instanceof ServerLevel lv) {
    	    SoundType soundtype = mob_pl.getBlockState().getSoundType(lv, mob.blockPosition(), null);
            lv.playSound(null, mob.blockPosition(), soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            particle(lv, mob.getX(), mob.getY(), mob.getZ(), mob_pl.getBlockState(), mob_pl.getShape());
    	}
   }

   public static Entity getEntityLookedAt(Player player, double distance) {
   	    EntityHitResult hit = getPlayerLookedResult(player, distance);
   	    if (hit != null) {
   	    	return hit.getEntity();
   	    }
   	    return null;
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


   private static void particle(ServerLevel world, double x, double y, double z, BlockState blockState, VoxelShape shape) {
    if (!blockState.isAir()) {
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

   @OnlyIn(Dist.CLIENT)
   public static void crackBlock(PlayerAccessor player, Direction dir) {
      Entity pl = (Player)player;
      Level level = pl.level();
      BlockState blockstate = player.getBlockState();
      if (blockstate.getRenderShape() != RenderShape.INVISIBLE && level instanceof ClientLevel lv) {
         double i = pl.getX();
         double j = pl.getY();
         double k = pl.getZ();
         float f = 0.1F;
         AABB aabb2 = pl.getBoundingBox();
         System.out.println(aabb2);
         AABB aabb = blockstate.getShape(level, pl.blockPosition()).bounds();
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
}
