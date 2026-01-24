package net.blockomorph.platformUtilsImpl;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.compat.FabricModelOnForge;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class NeoClientUtils implements ClientPlatformUtils {
	private static final Supplier<Minecraft> MC = () -> GuiUtils.MC;
	@Override
	public void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output, CreativeModeTab.DisplayItemsGenerator orig) {
		EventHooks.onCreativeModeTabBuildContents(tab, key, orig, parameters, output);
	}

	@Override
	public FogLiquidModifier.LiquidFogData calculateData(Level lv, BlockInPlayer2 block) {
		return ClientPlatformUtils.super.calculateData(lv, block);//TODO!!!!!!!!!
	}

	@Override
	public @Nullable TextureAtlasSprite[] getPlatformFluidSprite(Level lv, BlockInPlayer2 block) {
		return FluidSpriteCache.getFluidSprites(lv, block.getPos(), block.getBlockState().getFluidState());
	}

	@Override
	public Integer getPlatformFluidTint(Level lv, BlockInPlayer2 block) {
		return IClientFluidTypeExtensions.of(block.getBlockState().getFluidState()).getTintColor(block.getBlockState().getFluidState(), lv, block.getPos());
	}

	@Override
	public void renderBlockInWorld(boolean translucent, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl, BlockInPlayer2 block, RandomSource randomSource) {
		Level level = pl.player().level();
		BlockState blockstate = block.getBlockState();
		var model = MC.get().getBlockRenderer().getBlockModel(blockstate);
		BlockPos offset = block.getPos();
		var modeldata = model.getModelData(level, block.getPos(), blockstate, this.extractModelData(block));
		boolean redirectToFabric = model instanceof FabricModelOnForge acc && acc.isNotVanilla$blockomorph();
		for (RenderType renderType : model.getRenderTypes(blockstate, randomSource, modeldata)) {
			if (translucent != (renderType == RenderType.translucent())) return;
			VertexConsumer vertex = buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
			ModelBlockRenderer renderer = MC.get().getBlockRenderer().getModelRenderer();
			if (redirectToFabric) {
				renderer.tesselateBlock(level, model, blockstate, offset, posestack, vertex, true, randomSource, blockstate.getSeed(offset), OverlayTexture.NO_OVERLAY);
			} else {
				renderer.tesselateBlock(level, model, blockstate, offset, posestack, vertex, true, randomSource, blockstate.getSeed(offset), OverlayTexture.NO_OVERLAY, modeldata, renderType);
			}
		}
	}

	@Override
	public void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {
		RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
		if (blockState.getRenderShape() == RenderShape.MODEL && MC.get().level != null) {
			ClientLevelAccessor acc = ClientLevelAccessor.of(MC.get().level);
			acc.setSpecialRenderingMode(true);
			var model = MC.get().getBlockRenderer().getBlockModel(blockState);
			var modeldata = model.getModelData(MC.get().level, zeroOrFake, blockState, ModelData.EMPTY);
			for (var renderType : model.getRenderTypes(blockState, random, modeldata)) {
				VertexConsumer vertex = bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
				MC.get().getBlockRenderer().getModelRenderer().tesselateBlock(MC.get().level, model, blockState, zeroOrFake, stack, vertex, false, random, blockState.getSeed(zeroOrFake), OverlayTexture.NO_OVERLAY, modeldata, renderType);
			}
			acc.setSpecialRenderingMode(false);
		}
	}

	@Override
	public void renderBrake(PoseStack posestack, VertexConsumer buffer, PlayerAccessor pl, BlockInPlayer2 block, RandomSource randomSource) {
		BlockState blockstate = block.getBlockState();
		BlockPos pos = block.getPos();
		MC.get().getBlockRenderer().getModelRenderer().tesselateBlock(pl.player().level(), MC.get().getBlockRenderer().getBlockModel(blockstate), blockstate, pos, posestack, buffer, false, randomSource, blockstate.getSeed(pos), OverlayTexture.NO_OVERLAY, this.extractModelData(block), null);
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.hasSearchBar();
	}

	private ModelData extractModelData(BlockInPlayer2 block) {
		ModelData modelData = ModelData.EMPTY;
		if (block.getModelData() instanceof ModelData data) {
			modelData = data;
		}
		return modelData;
	}

	@Override
	public RenderType bakeGuiShader(ResourceLocation texture) {
		return RenderType.create(
				"gui_texture_with_alpha",
				DefaultVertexFormat.POSITION_TEX,
				VertexFormat.Mode.QUADS, 786432, RenderType.CompositeState.builder().setTextureState(
						new RenderStateShard.TextureStateShard(texture, false, false)
				).setShaderState(RenderType.POSITION_TEX_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).createCompositeState(false));
	}

	@Override
	public void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state) {
		particle.updateSprite(state, keyPos);
	}

	@Override
	public boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine) {
		return !IClientBlockExtensions.of(state).addDestroyEffects(state, level, keyPos, engine);
	}
}
