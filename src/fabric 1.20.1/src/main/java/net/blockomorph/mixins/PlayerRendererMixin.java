package net.blockomorph.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.screens.BlockMorphConfigScreen;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;


@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
   private final ItemInHandRenderer itemRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
   private final EntityRenderDispatcher entityDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
   private final BlockPos AIR = new BlockPos(0, 512, 0); 

   private final Minecraft mc = Minecraft.getInstance();

   public PlayerRendererMixin(EntityRendererProvider.Context p_174557_, boolean p_174558_) {
      super(p_174557_, new PlayerModel<>(p_174557_.bakeLayer(p_174558_ ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), p_174558_), 0.5F);
   }
   
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(AbstractClientPlayer player, float anim, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, CallbackInfo info) {
      if (player instanceof PlayerAccessor pl) {
      	if (pl.isFullActive()) {
      	   info.cancel();
      	   posestack.pushPose();
      	   this.adjustMatrixForPlayer(posestack, pl, player);
      	   this.renderBlock(player, posestack, buffer, pl);
           this.renderBlockEntity(player, partialticks, posestack, buffer, light, pl);
           this.renderBreak(player, posestack, buffer, pl);
           this.renderFrame(player, posestack, buffer, pl);
           posestack.popPose();
           this.shadowRadius = 0.0f;
      	} else {
            if (pl.getTnt() != null) {
                info.cancel();
                this.renderTnt(player, anim, partialticks, posestack, buffer, light, pl);
            }
            this.shadowRadius = 0.5f;
        }
      }
   }

   private void renderTnt(AbstractClientPlayer player, float anim, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
        PrimedTnt tnt = pl.getTnt();
        EntityRenderer<? super PrimedTnt> rend = entityDispatcher.getRenderer(pl.getTnt());
        try {
            rend.render(tnt, anim, partialticks, posestack, buffer, light);
        } catch (Exception e) {
            if (player == Minecraft.getInstance().player && Minecraft.getInstance().screen instanceof BlockMorphConfigScreen sc)
                sc.tagException = e.getMessage();
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
            this.renderBlockEntity(entry.getValue(), player, partialticks, posestack, buffer, this.getRenderLight(player, offset), pl, offset); //getRenderLight USE TO FIX LIGHT
      		posestack.popPose();
   	    }
   }

   private void renderBlockEntity(BlockState blockstate, AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl, BlockPos offset) {
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(offset, blockstate);
            if (blockEntity != null) {
              try {
      	        blockEntity.setLevel(player.level());
      	        blockEntity.load(pl.getTag());
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

   protected int getRenderLight(AbstractClientPlayer entity, BlockPos blockPos) {
        return LightTexture.pack(this.getBlockLightLevel(entity, blockPos), this.getSkyLightLevel(entity, blockPos));
   }

   private void renderBreak(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
   	    CompoundTag k = pl.getProgress();
   	    for (String key : k.getAllKeys()) {
            if (key.equals("fuse")) continue;
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
            VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1.pose(), posestack$pose1.normal(), 1.0F);
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
        this.renderVoxelShape(posestack, buffer.getBuffer(RenderType.lines()), shape);
        posestack.popPose();
   	}
   }

   private void renderVoxelShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape voxelShape) {
   	    PoseStack.Pose pose = poseStack.last();
        voxelShape.forAllEdges((k, l, m, n, o, p) -> {
            float q = (float)(n - k);
            float r = (float)(o - l);
            float s = (float)(p - m);
            float t = Mth.sqrt(q * q + r * r + s * s);
            vertexConsumer.vertex(pose.pose(), (float)(k), (float)(l), (float)(m)).color(0f, 0f, 0f, 0.4f).normal(pose.normal(), q /= t, r /= t, s /= t).endVertex();
            vertexConsumer.vertex(pose.pose(), (float)(n), (float)(o), (float)(p)).color(0f, 0f, 0f, 0.4f).normal(pose.normal(), q, r, s).endVertex();
        });
   }

}
