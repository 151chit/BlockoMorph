package net.blockomorph.platformUtilsImpl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
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
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoClientUtils implements ClientPlatformUtils {
	private static final Supplier<Minecraft> MC = () -> GuiUtils.MC;
	private static final List<BlockModelPart> cachedBreakingList = new ArrayList<>();
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
		List<BlockModelPart> modelParts = model.collectParts(level, offset, blockstate, randomSource).stream().filter((blockModelPart -> {
			return (blockModelPart.getRenderType(blockstate) == RenderType.translucent()) == translucent;
		})).toList();//modeldata include if need from blockandtintgetter.getModelData(pos) with mixin redirect
		Function<RenderType, VertexConsumer> bufferLookup = (renderType) -> {
			return buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
		};
		boolean redirectToFabric = model instanceof FabricModelOnForge acc && acc.isNotVanilla$blockomorph();
		ModelBlockRenderer renderer = MC.get().getBlockRenderer().getModelRenderer();
		randomSource.setSeed(blockstate.getSeed(offset));
		if (redirectToFabric) {
			renderer.tesselateBlock(level, modelParts, blockstate, offset, posestack, bufferLookup.apply(ItemBlockRenderTypes.getMovingBlockRenderType(blockstate)), true, OverlayTexture.NO_OVERLAY);
		} else {
			renderer.tesselateBlock(level, modelParts, blockstate, offset, posestack, bufferLookup, true, OverlayTexture.NO_OVERLAY);
		}
	}

	@Override
	public void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {
		RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
		Level level = MC.get().level;
		if (blockState.getRenderShape() == RenderShape.MODEL && level != null) {
			BlockRenderDispatcher blockRenderer = MC.get().getBlockRenderer();
			List<BlockModelPart> list = blockRenderer.getBlockModel(blockState).collectParts(level, zeroOrFake, blockState, random);
			Function<RenderType, VertexConsumer> bufferLookup = (renderType) -> {
				return bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
			};
			ClientLevelAccessor acc = ClientLevelAccessor.of(level);
			acc.setSpecialRenderingMode(true);
			blockRenderer.getModelRenderer().tesselateBlock(level, list, blockState, zeroOrFake, stack, bufferLookup, false, OverlayTexture.NO_OVERLAY);
			acc.setSpecialRenderingMode(false);
		}
	}

	@Override
	public void renderBrake(PoseStack posestack, VertexConsumer buffer, PlayerAccessor pl, BlockInPlayer2 block, RandomSource randomSource) {
		BlockState blockstate = block.getBlockState();
		BlockPos pos = block.getPos();
		var model = MC.get().getBlockRenderer().getBlockModel(blockstate);
		cachedBreakingList.clear();
		model.collectParts(pl.player().level(), pos, blockstate, RandomSource.create(blockstate.getSeed(pos)), cachedBreakingList);
		MC.get().getBlockRenderer().getModelRenderer().tesselateBlock(pl.player().level(), cachedBreakingList, blockstate, pos, posestack, (r) -> buffer, false, OverlayTexture.NO_OVERLAY);
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
}
