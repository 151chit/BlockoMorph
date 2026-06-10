package net.blockomorph.screens.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.phys.AABB;

public class DynamicSizeItemStackRenderState extends TrackingItemStackRenderState {
	private final AABB aabb;
	private final float sizeMultiplier;

	public DynamicSizeItemStackRenderState(float scale) {
		float size2 = scale / 2;
		this.aabb = new AABB(-size2, -size2, 0, size2, size2, 0);
		this.sizeMultiplier = scale / 16;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int outlineColor) {
		poseStack.scale(this.sizeMultiplier, this.sizeMultiplier, this.sizeMultiplier);
		super.submit(poseStack, submitNodeCollector, light, overlay, outlineColor);
	}

	@Override
	public boolean isOversizedInGui() {
		return true;
	}

	@Override
	public AABB getModelBoundingBox() {
		return this.aabb;
	}
}
