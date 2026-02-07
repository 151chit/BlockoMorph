package net.blockomorph.platformUtilsImpl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.compat.FabricModelOnForge;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
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
	public void submitBlockInWorld(boolean translucent, BlockAndTintGetter level, BlockState blockstate, BlockPos keyPos, PoseStack posestack, SubmitNodeCollector collector, RandomSource randomSource) {
		var model = MC.get().getBlockRenderer().getBlockModel(blockstate);
		if (model instanceof FabricModelOnForge acc && acc.isNotVanilla$blockomorph()) {
			ClientPlatformUtils.super.submitBlockInWorld(translucent, level, blockstate, keyPos, posestack, collector, randomSource);
			return;
		}

		randomSource.setSeed(blockstate.getSeed(keyPos));
		EnumMap<ChunkSectionLayer, List<BlockModelPart>> partsByShader = new EnumMap<>(ChunkSectionLayer.class);
		model.collectParts(level, keyPos, blockstate, randomSource).forEach(blockModelPart -> {
			ChunkSectionLayer layer = blockModelPart.getRenderType(blockstate);
			if (this.needChangeToCutout(blockstate)) layer = ChunkSectionLayer.CUTOUT;
			if ((layer == ChunkSectionLayer.TRANSLUCENT) == translucent) {
				partsByShader.computeIfAbsent(layer, l -> new ArrayList<>()).add(blockModelPart);
			}
		});//modeldata include if need from blockandtintgetter.getModelData(pos) with mixin redirect

		ModelBlockRenderer renderer = MC.get().getBlockRenderer().getModelRenderer();
		for (ChunkSectionLayer layer : partsByShader.keySet()) {
			collector.submitCustomGeometry(posestack, RenderTypeHelper.getMovingBlockRenderType(layer), (pose, vertexconsumer) -> {
				PoseStack poseStack = new PoseStack();
				poseStack.last().set(pose);
				renderer.tesselateBlock(level, partsByShader.get(layer), blockstate, keyPos, poseStack, l -> vertexconsumer, true, OverlayTexture.NO_OVERLAY);
			});
		}
	}

	@Override
	public void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {
		RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
		Level level = MC.get().level;
		if (blockState.getRenderShape() == RenderShape.MODEL && level != null) {
			BlockRenderDispatcher blockRenderer = MC.get().getBlockRenderer();
			List<BlockModelPart> modelParts = blockRenderer.getBlockModel(blockState).collectParts(level, zeroOrFake, blockState, random);
			Function<ChunkSectionLayer, VertexConsumer> bufferLookup = (renderType) -> {
				return bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
			};
			ClientLevelAccessor acc = ClientLevelAccessor.of(level);
			acc.setSpecialRenderingMode(true);
			blockRenderer.getModelRenderer().tesselateBlock(level, modelParts, blockState, zeroOrFake, stack, bufferLookup, false, OverlayTexture.NO_OVERLAY);
			acc.setSpecialRenderingMode(false);
		}
	}

	@Override
	public boolean hasSearchBarInTab(CreativeModeTab tab) {
		return tab.hasSearchBar();
	}

	@Override
	public void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state) {
		particle.updateSprite(state, keyPos);
	}

	@Override
	public boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine) {
		return !IClientBlockExtensions.of(state).addDestroyEffects(state, level, keyPos, engine);
	}

	@Override @Nullable
	public ScreenRectangle scissorsPeek(GuiGraphics guiGraphics) {
		return guiGraphics.peekScissorStack();
	}

	@Override
	public void submitCustomPipRenderState(GuiGraphics guiGraphics, PictureInPictureRenderState renderState) {
		guiGraphics.submitPictureInPictureRenderState(renderState);
	}
}
