package net.blockomorph.core.render.layers;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

public enum PlayerSectionLayerGroup {
	OPAQUE(PlayerSectionLayer.SOLID, PlayerSectionLayer.CUTOUT),
	TRANSLUCENT(PlayerSectionLayer.TRANSLUCENT);

	final PlayerSectionLayer[] layers;

	PlayerSectionLayerGroup(final PlayerSectionLayer... layers) {
		this.layers = layers;
	}

	public PlayerSectionLayer[] layers() {
		return this.layers;
	}

	public RenderTarget getOutputTarget() {
		var translucentTarget = GuiUtils.MC.levelRenderer.getTranslucentTarget();
		return switch (this) {
			case OPAQUE -> GuiUtils.MC.getMainRenderTarget();
			case TRANSLUCENT -> translucentTarget != null ? translucentTarget : OPAQUE.getOutputTarget();
		};
	}

	public static PlayerSectionLayerGroup byChunkType(ChunkSectionLayerGroup layer) {
		return switch (layer) {
			case OPAQUE -> OPAQUE;
			case TRANSLUCENT -> TRANSLUCENT;
		};
	}
}
