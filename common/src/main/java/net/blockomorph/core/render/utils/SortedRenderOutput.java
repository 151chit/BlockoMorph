package net.blockomorph.core.render.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

public interface SortedRenderOutput {
	void appendCustomVertex(PoseStack forCopy, RenderType renderType, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor);
	void appendChunkMovingBlocks(PoseStack stack, PlayerSectionLayer layer, BiConsumer<PoseStack.Pose, VertexConsumer> vertexAcceptor);
	void appendBreaks(PoseStack poseStack, BlockState block, long seed, int progress);
	<T> void appendEntityObject(T gameObjectOrState, PoseStack stack);
}
