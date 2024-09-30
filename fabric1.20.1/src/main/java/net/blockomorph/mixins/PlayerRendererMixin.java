package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.MorphUtils;
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


@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
   private final ItemInHandRenderer itemRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
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
      	   //this.rotate(posestack, player.getDirection().getOpposite());
      	   this.renderBlock(player, posestack, buffer, pl);
           this.renderBlockEntity(player, partialticks, posestack, buffer, light, pl);
           this.renderBreak(player, posestack, buffer, pl);
           this.renderFrame(player, posestack, buffer, pl);
           //this.renderTool(player, posestack, buffer, light);
           this.shadowRadius = 0.0f;
      	} else {
        	this.shadowRadius = 0.5f;
        }
      }
   }

   private void rotate(PoseStack poseStack, Direction dir) {
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
   }

   private void renderBlock(AbstractClientPlayer player, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl) {
      	BlockState blockstate = pl.getBlockState();
      	Level level = player.level();
      	BlockPos pos = player.blockPosition();
        RandomSource random = RandomSource.create(blockstate.getSeed(pos));
        posestack.pushPose();
        posestack.translate(-0.5D, 0.0D, -0.5D);
        var model = this.dispatcher.getBlockModel(blockstate);
        var renderType = ItemBlockRenderTypes.getMovingBlockRenderType(blockstate);
        this.dispatcher.getModelRenderer().tesselateBlock(level, model, blockstate, pos, posestack, buffer.getBuffer(renderType), false, RandomSource.create(), blockstate.getSeed(pos), OverlayTexture.NO_OVERLAY);
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
