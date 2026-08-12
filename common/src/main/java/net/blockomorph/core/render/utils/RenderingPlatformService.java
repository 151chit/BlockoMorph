package net.blockomorph.core.render.utils;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.blockGetter.ProxyPlayerBlockGetter;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ServiceLoader;

public interface RenderingPlatformService {
	RenderingPlatformService INSTANCE = ServiceLoader.load(RenderingPlatformService.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load RENDERING platform-depended utils, mod cannot run!"));

	PlatformBlockTesselator createBlockTessellator();
	ProxyPlayerBlockGetter createBlocksSnapshotHolderForAsync(InPlayerManager manager);
	BlocksRenderState crateBlocksSnapshotRenderState(InPlayerManager manager, int maxCapacity);

	TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state);
	default Integer tintForBlock(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		var tint = GuiUtils.MC.getBlockColors().getTintSource(state, 0);
		if (tint != null) {
			return tint.colorInWorld(state, level, pos);
		}
		return null;
	}
}
