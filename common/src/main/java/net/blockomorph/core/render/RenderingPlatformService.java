package net.blockomorph.core.render;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.blockGetter.ProxyPlayerBlockGetter;
import net.blockomorph.core.render.renderers.PlatformBlockTesselator;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
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

	@SuppressWarnings("unused") default TextureAtlasSprite[] spritesForFluid(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		var model = GuiUtils.MC.getModelManager().getFluidStateModelSet().get(state.getFluidState());
		var sprite = new TextureAtlasSprite[3];
		sprite[0] = this.checkForSprite(model.stillMaterial());
		sprite[1] = this.checkForSprite(model.flowingMaterial());
		sprite[2] = this.checkForSprite(model.overlayMaterial());
		return sprite;
	}

	private TextureAtlasSprite checkForSprite(Material.Baked material) {
		return material != null ? material.sprite() : null;
	}
}
