package net.blockomorph.platformImpl.rendering;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.render.utils.RenderingPlatformService;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.blockGetter.ProxyPlayerBlockGetter;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.model.data.ModelData;

public class NeoRenderService implements RenderingPlatformService {

	@Override
	public PlatformBlockTesselator createBlockTessellator() {
		return new NeoForgeBlockRenderer();
	}

	@Override
	public ProxyPlayerBlockGetter createBlocksSnapshotHolderForAsync(InPlayerManager manager) {
		return new ProxyPlayerBlockGetter(manager) {
			@Override
			public ModelData extractRenderDataFrom(BlockEntity blockEntity) {
				if (blockEntity == null) return ModelData.EMPTY;
				return blockEntity.getModelData();
			}

			@Override
			public ModelData getModelData(BlockPos pos) {
				Object obj = this.getModelDataRaw(pos);
				if (obj instanceof ModelData data) {
					return data;
				}
				return ModelData.EMPTY;
			}
		};
	}

	@Override
	public BlocksRenderState crateBlocksSnapshotRenderState(InPlayerManager manager, int maxCapacity) {
		return new BlocksRenderState(manager, maxCapacity) {
			@Override
			public ModelData extractRenderDataFrom(BlockEntity blockEntity) {
				if (blockEntity == null) return ModelData.EMPTY;
				return blockEntity.getModelData();
			}

			@Override
			public ModelData getModelData(BlockPos pos) {
				Object obj = this.getModelDataRaw(pos);
				if (obj instanceof ModelData data) {
					return data;
				}
				return ModelData.EMPTY;
			}
		};
	}

	@Override
	public Integer tintForBlock(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof LiquidBlock) {
			int tint = IClientFluidTypeExtensions.of(state.getFluidState()).getTintColor(state.getFluidState(), level, pos);
			if (tint == -1) return null;
			return tint;
		}
		return RenderingPlatformService.super.tintForBlock(level, pos, state);
	}

	@Override
	public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		var model = GuiUtils.MC.getModelManager().getBlockModelShaper().getBlockModel(state);
		return model.particleIcon(level, pos, state);
	}
}
