package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.*;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.render.utils.SortedRenderOutput;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.blockomorph.core.render.dispatch.MorphedRenderStateExtractor;
import net.blockomorph.core.render.renderers.async.AsyncRenderersStorage;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class MorphedMainRenderer implements MorphedRenderer {
	private final MorphedRenderStateExtractor stateExtractor;
	private final DirectBlocksRenderer directBlocksRenderer;
	private final ClientInPlayerManager manager;
	private AsyncRenderersStorage asyncBlocksRenderers;

	public MorphedMainRenderer(ClientInPlayerManager manager, int heavyMode) {
		this.manager = manager.assertOnInit();
		this.stateExtractor = new MorphedRenderStateExtractor(manager, heavyMode, this::tryConnectAsyncStorage);
		this.directBlocksRenderer = new DirectBlocksRenderer(manager.getOwnerUUID());
	}

	private AsyncRenderersStorage tryConnectAsyncStorage() {
		var manager = PlayersAsyncBakersManager.Provider.getFromVanilla();
		if (this.asyncBlocksRenderers == null || this.asyncBlocksRenderers.destroyed())
			this.asyncBlocksRenderers = manager.allocateOrGetFor(this.manager.getOwnerUUID());
		return this.asyncBlocksRenderers;
	}

	public MorphedRenderStateExtractor getStateExtractor() {
		return this.stateExtractor;
	}

	public void submit(PoseStack posestack, MorphedPlayerRenderState renderState, SortedRenderOutput collector) {
		if (renderState instanceof MorphedPlayerRenderState.BlockMorph blockMorph) {
			posestack.pushPose();
			posestack.translate(blockMorph.matrixOffsetX, 0, blockMorph.matrixOffsetZ);
			this.directBlocksRenderer.submit(renderState, posestack, collector);
			this.submitAdditional(posestack, collector, blockMorph);
			this.submitFrame(blockMorph, posestack, collector);
			posestack.popPose();
		} else if (renderState instanceof MorphedPlayerRenderState.TntMorph tntMorph) {
			collector.appendEntityObject(tntMorph.tntRenderState, posestack);
		}
	}

	private void submitAdditional(PoseStack posestack, SortedRenderOutput collector, MorphedPlayerRenderState.BlockMorph blockMorph) {
		var additionalList = blockMorph.additionalBlockData;
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < additionalList.size(); i++) {
			var additionalData = additionalList.get(i);
			posestack.pushPose();
			posestack.translate(additionalData.pos().getX(), additionalData.pos().getY(), additionalData.pos().getZ());
			try {
				var blockEntityRenderState = additionalData.blockEntity();
				if (blockEntityRenderState != null)
					collector.appendEntityObject(blockEntityRenderState, posestack);
				this.submitBrakes(posestack, collector, additionalData.blockState(), additionalData.blockPos(), additionalData.brakeProgress());
			} finally {
				posestack.popPose();
			}
		}
	}

	private void submitBrakes(PoseStack posestack, SortedRenderOutput collector, BlockState blockState, BlockPos pos, int breaks) {
		if (breaks < 0 || breaks> 9) return;
		collector.appendBreaks(posestack, blockState, blockState.getSeed(pos), breaks);
	}

	private void submitFrame(MorphedPlayerRenderState.BlockMorph state, PoseStack posestack, SortedRenderOutput collector) {
		VoxelShape frameShape = state.outLineShape;
		InPlayerBlockPos framePos = state.outLinePos;
		if (frameShape != null && framePos != null) {
			posestack.pushPose();
			posestack.translate(framePos.x, framePos.y, framePos.z);
			int color = GuiUtils.MC.options.highContrastBlockOutline().get() ? -11010079 : ARGB.color(102, -16777216);
			float width = GuiUtils.MC.getWindow().getAppropriateLineWidth();
			collector.appendCustomVertex(posestack, RenderTypes.LINES, ((pose, vertexConsumer) ->
				frameShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
					Vector3f normal = new Vector3f((float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1)).normalize();
					vertexConsumer.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
					vertexConsumer.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
				})
			));
			posestack.popPose();
		}
	}
}
