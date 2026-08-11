package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.blockomorph.core.render.utils.SortedRenderOutput;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DirectBlocksRenderer extends BakedBlocksRenderer {
	private final EnumMap<PlayerSectionLayer, VertexRecorder> buffers = new EnumMap<>(PlayerSectionLayer.class);
	private final Vector3f mutableVec = new Vector3f();
	private PoseStack currentStack;

	protected DirectBlocksRenderer(UUID id) {
		super(id);
	}

	public void submit(MorphedPlayerRenderState state, PoseStack stack, SortedRenderOutput collector) {
		if (!(state instanceof MorphedPlayerRenderState.BlockMorph bm)) return;
		this.currentStack = stack;
		var blocksData = bm.immediateBlocksData;
		if (blocksData == null) return;
		this.initOnMainThread();
		for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			this.buffers.put(layer, new VertexRecorder() {
				@Override
				public VertexConsumer addVertex(float x, float y, float z) {
					Vector3f pos = currentStack.last().pose().transformPosition(x, y, z, mutableVec);
					return super.addVertex(pos.x, pos.y, pos.z);
				}

				@Override
				public VertexConsumer setNormal(float x, float y, float z) {
					Vector3f pos = currentStack.last().pose().transformPosition(x, y, z, mutableVec);
					return super.setNormal(pos.x, pos.y, pos.z);
				}
			});
		}
		this.renderBlocks(blocksData);
		for (PlayerSectionLayer layer : PlayerSectionLayer.values()) {
			VertexRecorder recorder = this.buffers.get(layer);
			collector.appendChunkMovingBlocks(stack, layer, (ignored, buffer) ->
					recorder.replay(buffer));
		}
		this.getCollectedAnimatedSprites().forEach(this::activateSprite);
		this.buffers.clear();
		this.currentStack = null;
	}

	@Override
	protected VertexConsumer getOutput(PlayerSectionLayer layer) {
		return this.buffers.get(layer);
	}

	private static class VertexRecorder implements VertexConsumer {
		final List<Consumer<VertexConsumer>> commands = new ObjectArrayList<>();

		void replay(VertexConsumer realBuffer) {
			for (Consumer<VertexConsumer> command : this.commands) {
				command.accept(realBuffer);
			}
			this.commands.clear();
		}

		@Override public VertexConsumer addVertex(float x, float y, float z) { this.commands.add(buffer -> buffer.addVertex(x, y, z)); return this; }
		@Override public VertexConsumer setColor(int r, int g, int b, int a) { this.commands.add(buffer -> buffer.setColor(r, g, b, a)); return this; }
		@Override public VertexConsumer setColor(int color) { this.commands.add(buffer -> buffer.setColor(color)); return this; }
		@Override public VertexConsumer setUv(float u, float v) { this.commands.add(buffer -> buffer.setUv(u, v)); return this; }
		@Override public VertexConsumer setUv1(int u, int v) { this.commands.add(buffer -> buffer.setUv1(u, v)); return this; }
		@Override public VertexConsumer setUv2(int u, int v) { this.commands.add(buffer -> buffer.setUv2(u, v)); return this; }
		@Override public VertexConsumer setNormal(float x, float y, float z) { this.commands.add(buffer -> buffer.setNormal(x, y, z)); return this; }
		@Override public VertexConsumer setLineWidth(float width) { this.commands.add(buffer -> buffer.setLineWidth(width)); return this; }
	}
}
