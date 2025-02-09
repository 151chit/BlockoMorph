package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.blockomorph.utils.*;
import net.blockomorph.core.*;
import net.blockomorph.screens.BlockMorphConfigScreen;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.GameType;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Debug;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.ShapeRenderer;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

//@Debug(export = true)
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> 
extends EntityRenderer<T, S> {
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
   private final ItemInHandRenderer itemRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
   private final BlockPos AIR = new BlockPos(0, 512, 0); 

   private final Minecraft mc = Minecraft.getInstance();

   public PlayerRendererMixin(EntityRendererProvider.Context context, M entityModel, float f) {
      super(context);
   }

   private RenderStateAccessor getPl(PlayerRenderState r) {
   	  return (RenderStateAccessor)r;
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void extractRenderState(T abstractClientPlayer, S playerRenderState, float f, CallbackInfo ci) {
   	  if (playerRenderState instanceof PlayerRenderState r)
   	      this.getPl(r).loadPlayer((AbstractClientPlayer)abstractClientPlayer);
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(S state, PoseStack posestack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
      if (state instanceof PlayerRenderState r && this.getPl(r).getPlayer() instanceof PlayerAccessor pl) {
      	if (pl.isActive()) { 
      	   ci.cancel();
      	   AbstractClientPlayer player = (AbstractClientPlayer)pl;
      	   posestack.pushPose();
      	   this.adjustMatrixForPlayer(posestack, pl, player);
      	   this.renderBlock(player, posestack, buffer, pl);
      	   float g = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(!Minecraft.getInstance().level.tickRateManager().isEntityFrozen(player));
           this.renderBlockEntity(player, g, posestack, buffer, packedLight, pl);
           this.renderBreak(player, posestack, buffer, pl);
           this.renderFrame(player, posestack, buffer, pl);
           posestack.popPose();
           this.shadowRadius = 0.0f;
      	} else {
           this.shadowRadius = 0.5f;
        }
      }
   }

   private void adjustMatrixForPlayer(PoseStack poseStack, PlayerAccessor pl, AbstractClientPlayer player) {
        BlockPos minpos = pl.minPos();
        AABB hitbox = player.getBoundingBox();
        Vec3 playerCenter = player.position();
        double hitboxMinX = hitbox.minX;
        double hitboxMinZ = hitbox.minZ;

        double offsetX = hitbox.minX - (playerCenter.x + minpos.getX());
        double offsetZ = hitbox.minZ - (playerCenter.z + minpos.getZ());

        poseStack.translate(offsetX, 0.0, offsetZ);
   }

   /*private void rotate(PoseStack poseStack, Direction dir) {
    	switch (dir) {
    	        case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
				case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
				case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
    	};
   }

   private void renderTool(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, int light) {
   	    ItemStack right = player.getMainHandItem();
        ItemStack left = player.getOffhandItem();
        if (right != null && !right.isEmpty())
   	       this.renderItems(posestack, buffer, light, true, right, player);
   	    if (left != null && !left.isEmpty())
   	       this.renderItems(posestack, buffer, light, false, left, player);
   }

   private void renderItems(PoseStack posestack, MultiBufferSource buffer, int light, boolean right, ItemStack stack, AbstractClientPlayer player) {
   	    posestack.pushPose();
   	    int swing = 10;
        if (player.swinging) swing = swing + player.swingTime * 15;
   	    posestack.mulPose(Axis.XP.rotationDegrees(swing));
   	    posestack.mulPose(Axis.XP.rotationDegrees(-90.0F));
   	    posestack.translate((!right ? -8.5F : 8.5F) / 16.0F, 0.5F, 0.0F);
   	    itemRenderer.renderItem(player, stack, right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, false, posestack, buffer, light);
   	    posestack.popPose();
   }*/

   private void renderBlock(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
      	BlockState blockstate = pl.getBlockState();
      	this.renderBlock(player, blockstate, posestack, buffer, player.blockPosition());
      	for (Map.Entry<BlockPos, BlockState> entry : pl.getBlocks().entrySet()) {
      		BlockPos pos = entry.getKey();
      		posestack.pushPose();
            posestack.translate(pos.getX(), pos.getY(), pos.getZ());
            this.renderBlock(player, entry.getValue(), posestack, buffer, player.blockPosition().offset(pos));
      		posestack.popPose();
      	}
   }

   private void renderBlock(AbstractClientPlayer player, BlockState blockstate, PoseStack posestack, MultiBufferSource buffer, BlockPos offset) {
   	    Level level = player.level();
   	    BlockPos pos = offset;// NEED REPAIR TO FIX LIGHT!!!!!!
   	    posestack.pushPose();
        var model = this.dispatcher.getBlockModel(blockstate);
        var renderType = ItemBlockRenderTypes.getMovingBlockRenderType(blockstate);
        this.dispatcher.getModelRenderer().tesselateBlock(level, model, blockstate, pos, posestack, buffer.getBuffer(renderType), false, RandomSource.create(), blockstate.getSeed(pos), OverlayTexture.NO_OVERLAY);
        posestack.popPose();
   }

   private void renderBlockEntity(AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
   	    BlockState blockstate = pl.getBlockState();
   	    this.renderBlockEntity(blockstate, player, partialticks, posestack, buffer, light, pl, player.blockPosition());
   	    for (Map.Entry<BlockPos, BlockState> entry : pl.getBlocks().entrySet()) {
   	    	BlockPos pos = entry.getKey();
      		posestack.pushPose();
            posestack.translate(pos.getX(), pos.getY(), pos.getZ());
            BlockPos offset = player.blockPosition().offset(pos);
            this.renderBlockEntity(entry.getValue(), player, partialticks, posestack, buffer, this.getRenderLight((T)player, offset), pl, offset); //getRenderLight USE TO FIX LIGHT
      		posestack.popPose();
   	    }
   }

   private void renderBlockEntity(BlockState blockstate, AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl, BlockPos offset) {
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(offset, blockstate);
            if (blockEntity != null) {
              try {
      	        blockEntity.setLevel(player.level());
      	        blockEntity.loadWithComponents(pl.getTag(), player.level().registryAccess());
                BlockEntityRenderer renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
                if (renderer != null) {
           	        posestack.pushPose();
                    renderer.render(blockEntity, partialticks, posestack, buffer, light, OverlayTexture.NO_OVERLAY);
                    posestack.popPose();
                }
              } catch (Exception e) {
           	    if (player == Minecraft.getInstance().player && Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc)	
           	        sc.tagException = e.getMessage();
              }
           }  
        }
   }

   protected int getRenderLight(T entity, BlockPos blockPos) {
        return LightTexture.pack(this.getBlockLightLevel(entity, blockPos), this.getSkyLightLevel(entity, blockPos));
   }

   private void renderBreak(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
   	    CompoundTag k = pl.getProgress();
   	    for (String key : k.getAllKeys()) {
   	    	posestack.pushPose();
   	    	
   	    	BlockPos pos = this.parseBlockPos(key);
   	    	BlockState state;
   	    	if (pos.equals(new BlockPos(0,0,0))) {
   	    		state = pl.getBlockState();
   	    	} else {
   	    		state = pl.getBlocks().get(pos);
   	    	}
   	    	if (pos != null && state != null) {
   	    		posestack.translate(pos.getX(), pos.getY(), pos.getZ());
   	    		this.renderBreak(k.getInt(key), state, player, posestack, buffer);
   	    	}

   	    	posestack.popPose();
   	    }
   }

   private void renderBreak(int k, BlockState blockstate, AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer) {
        posestack.pushPose();
        PoseStack.Pose posestack$pose1 = posestack.last();
        if (k > -1 && k < 10) {
            VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1, 1.0F);
            this.dispatcher.renderBreakingTexture(blockstate, AIR, player.level(), posestack, vertexconsumer1);
        }
 
        posestack.popPose();
   }

   @Nullable
   private BlockPos parseBlockPos(String input) {
        String[] parts = input.trim().split(" ");
    
        if (parts.length != 3) return null;
    
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
    
            return new BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
   }

   private void renderFrame(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
   	Minecraft mc = Minecraft.getInstance();
   	if (player == MorphUtils.hitEntity && 
   	    !mc.player.isSpectator() && 
   	    mc.gameMode.getPlayerMode() != GameType.ADVENTURE && 
   	    !mc.options.hideGui &&
   	    MorphUtils.hitPart != null
   	) {
   	    posestack.pushPose();
   	    VoxelShape shape = pl.getRenderShape(MorphUtils.hitPart);
   	    if (shape == null) {
   	    	posestack.popPose();
   	    	return;
   	    }
   	    Boolean boolean_ = mc.options.highContrastBlockOutline().get();
   	    int i = boolean_ != false ? -11010079 : ARGB.color(102, -16777216);
   	    VertexConsumer vertexConsumer;
   	    if (boolean_.booleanValue()) {
   	    	vertexConsumer = buffer.getBuffer(RenderType.secondaryBlockOutline());
   	    	ShapeRenderer.renderShape(posestack, vertexConsumer, shape, 0, 0, 0, -16777216);
   	    }
   	    vertexConsumer = buffer.getBuffer(RenderType.lines());
        ShapeRenderer.renderShape(posestack, vertexConsumer, shape, 0, 0, 0, i);
        posestack.popPose();
   	}
   }

}
