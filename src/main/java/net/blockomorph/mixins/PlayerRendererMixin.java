package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.LevelRendererAccessor;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.screens.BlockMorphConfigScreen;

import net.minecraftforge.client.model.data.ModelData;

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

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
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
      	if (pl.isActive()) { 
      	   info.cancel();
      	   this.renderBlock(player, posestack, buffer, pl);
           this.renderBlockEntity(player, partialticks, posestack, buffer, light, pl);
           this.renderBreak(player, posestack, buffer, pl);
           this.renderFrame(player, posestack, buffer, pl);
           this.shadowRadius = 0.0f;
      	} else {
        	this.shadowRadius = 0.5f;
        }
      }
   }

   private void renderBlock(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
      	BlockState blockstate = pl.getBlockState();
      	Level level = player.level();
      	BlockPos pos = player.blockPosition();
        RandomSource random = RandomSource.create(blockstate.getSeed(pos));
        posestack.pushPose();
        posestack.translate(-0.5D, 0.0D, -0.5D);
        var model = this.dispatcher.getBlockModel(blockstate);
        for (var renderType : model.getRenderTypes(blockstate, random, net.minecraftforge.client.model.data.ModelData.EMPTY))
           this.dispatcher.getModelRenderer().tesselateBlock(level, model, blockstate, pos, posestack, buffer.getBuffer(renderType), false, RandomSource.create(), blockstate.getSeed(pos), OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType);
        posestack.popPose();
   }

   private void renderBlockEntity(AbstractClientPlayer player, float partialticks, PoseStack posestack, MultiBufferSource buffer, int light, PlayerAccessor pl) {
     	BlockState blockstate = pl.getBlockState();
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(player.blockPosition(), blockstate);
            if (blockEntity != null) {
              try {
      	        blockEntity.setLevel(player.level());
      	        blockEntity.load(pl.getTag());
                BlockEntityRenderer renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
                if (renderer != null) {
           	        posestack.pushPose();
            	    posestack.translate(-0.5D, 0.0D, -0.5D);
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

   private void renderBreak(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
   	    BlockState blockstate = pl.getBlockState();
      	Level level = player.level();
        posestack.pushPose();

        int k = pl.getProgress();
        PoseStack.Pose posestack$pose1 = posestack.last();
        if (k > -1 && k < 10) {
            posestack.translate(-0.5, 0.0, -0.5);
            VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1.pose(), posestack$pose1.normal(), 1.0F);
            this.dispatcher.renderBreakingTexture(blockstate, AIR, player.level(), posestack, vertexconsumer1);
        }
 
        posestack.popPose();
   }

   private void renderFrame(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
   	Minecraft mc = Minecraft.getInstance();
   	if (player == MorphUtils.hitEntity && 
   	    !mc.player.isSpectator() && 
   	    mc.gameMode.getPlayerMode() != GameType.ADVENTURE && 
   	    !mc.options.hideGui
   	) {
   	    BlockState blockstate = pl.getBlockState();
   	    Block block = blockstate.getBlock();
   	    posestack.pushPose();
   	    posestack.translate(-0.5, 0.0, -0.5);
   	    VoxelShape shape = pl.getRenderShape();
        ((LevelRendererAccessor)mc.levelRenderer).renderBlockHitbox(posestack, buffer.getBuffer(RenderType.lines()), shape, 0, 0, 0, 0f, 0f, 0f, 0.4f);
        posestack.popPose();
   	}
   }

}
