package net.blockomorph.platformImpl.rendering;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.render.utils.RenderingPlatformService;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.blockGetter.ProxyPlayerBlockGetter;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FabricRenderService implements RenderingPlatformService {
	@Override
	public PlatformBlockTesselator createBlockTessellator() {
		return new FabricBlockRenderer();
	}

	@Override
	public ProxyPlayerBlockGetter createBlocksSnapshotHolderForAsync(InPlayerManager manager) {
		return new ProxyPlayerBlockGetter(manager) {
			@Override
			public Object extractRenderDataFrom(BlockEntity blockEntity) {
				if (blockEntity == null) return null;
				return blockEntity.getRenderData();
			}

			@Override
			public Object getBlockEntityRenderData(BlockPos pos) {
				return this.getModelDataRaw(pos);
			}
		};
	}

	@Override
	public BlocksRenderState crateBlocksSnapshotRenderState(InPlayerManager manager, int maxCapacity) {
		return new BlocksRenderState(manager, maxCapacity) {
			@Override
			public Object extractRenderDataFrom(BlockEntity blockEntity) {
				if (blockEntity == null) return null;
				return blockEntity.getRenderData();
			}

			@Override
			public Object getBlockEntityRenderData(BlockPos pos) {
				return this.getModelDataRaw(pos);
			}
		};
	}

	@Override
	public Integer tintForBlock(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof LiquidBlock) {
			var handler = FluidRenderHandlerRegistry.INSTANCE.get(state.getFluidState().getType());
			if (handler == null) return null;
			int color = handler.getFluidColor(level, pos, state.getFluidState());
			if (color == -1) return null;
			return color;
		}
		return RenderingPlatformService.super.tintForBlock(level, pos, state);
	}

	@Override
	public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		var model = GuiUtils.MC.getModelManager().getBlockModelShaper().getBlockModel(state);
		return model.particleSprite(level, pos, state);
	}
}
