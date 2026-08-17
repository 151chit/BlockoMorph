package net.blockomorph.core.render.renderers.async;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.renderer.Sheets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import oshi.annotation.concurrent.NotThreadSafe;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@NotThreadSafe
public class PlayerSectionMeshBuffer implements AutoCloseable {
	private final AutoResizeGpuBuffer vertexBuffer;
	private final AutoResizeGpuBuffer indexBuffer; boolean supportIndexes;
	private final Function<String, String> bufferName;
	@Nullable private final IntFunction<String> resizeMessage;
	private final PlayerSectionLayer layer;
	private MeshData.DrawState meshState;

	public PlayerSectionMeshBuffer(PlayerSectionLayer layer, Function<String, String> bufferName, @Nullable IntFunction<String> resizeMessage) {
		this.vertexBuffer = new AutoResizeGpuBuffer(BufferType.VERTICES, layer.baseBufferSize());
		this.indexBuffer = new AutoResizeGpuBuffer(BufferType.INDICES, layer.baseBufferSize());
		this.layer = layer;
		this.bufferName = bufferName;
		this.resizeMessage = resizeMessage;
	}

	public static void drawOnGpu(Iterable<PlayerSectionMeshBuffer> buffers, RenderTarget renderTarget, Vector3f cameraOffsetMatrix, Supplier<String> ignored) {
		var textureView = renderTarget.getColorTexture();
		if (textureView == null) return;
		var depthTextureView = renderTarget.getDepthTexture();
		if (depthTextureView == null) return;
		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				textureView, OptionalInt.empty(), depthTextureView, OptionalDouble.empty())) {
			for (PlayerSectionMeshBuffer buffer : buffers) {
				if (!buffer.isReadyToRender()) continue;
				MeshData.DrawState drawState = buffer.meshState;
				buffer.layer.pipeline().setupRenderState();
				renderPass.setPipeline(buffer.layer.pipeline().getRenderPipeline());
				Matrix4f old = new Matrix4f(RenderSystem.getModelViewMatrix());
				RenderSystem.getModelViewMatrix().translate(cameraOffsetMatrix);
				var blockAtlas = GuiUtils.MC.getTextureManager().getTexture(Sheets.BLOCKS_MAPPER.sheet());
				renderPass.bindSampler("Sampler0", blockAtlas.getTexture());
				renderPass.bindSampler("Sampler2", GuiUtils.MC.gameRenderer.lightTexture().getTarget());
				renderPass.setVertexBuffer(0, buffer.vertexBuffer.buffer);
				GpuBuffer indexBuf = buffer.indexBuffer.buffer;
				VertexFormat.IndexType indexType = drawState.indexType();
				if (!buffer.supportIndexes) {
					RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
					indexBuf = autoIndices.getBuffer(drawState.indexCount());
					indexType = autoIndices.type();
				}
				renderPass.setIndexBuffer(indexBuf, indexType);
				renderPass.drawIndexed(0, drawState.indexCount());
				buffer.layer.pipeline().clearRenderState();
				RenderSystem.getModelViewMatrix().set(old);
			}
		}
	}

	public boolean loadMeshData(MeshData meshData) {
		if (meshData != null) {
			this.vertexBuffer.uploadMeshDataPart(meshData.vertexBuffer());
			var index = meshData.indexBuffer();
			this.supportIndexes = index != null;
			if (this.supportIndexes)
				this.indexBuffer.uploadMeshDataPart(index);
			this.meshState = meshData.drawState();
			return true;
		} else {
			this.meshState = null;
			return false;
		}
	}

	public void loadIndexes(ByteBufferBuilder.Result byteBuffer) {
		this.indexBuffer.uploadMeshDataPart(byteBuffer.byteBuffer());
	}

	public boolean isReadyToRender() {
		return this.meshState != null;
	}

	@Override
	public void close() {
		this.vertexBuffer.buffer.close();
		this.indexBuffer.buffer.close();
	}

	private class AutoResizeGpuBuffer {
		private final BufferType usage;
		private GpuBuffer buffer;

		AutoResizeGpuBuffer(BufferType usage, int bufferCapacity) {
			this.usage = usage;
			this.createBuffer(bufferCapacity);
		}

		void createBuffer(int capacity) {
			if (this.buffer != null) this.buffer.close();
			this.buffer = RenderSystem.getDevice().createBuffer(() -> bufferName.apply(usage + ""),
					this.usage, BufferUsage.DYNAMIC_WRITE, capacity);
		}

		void uploadMeshDataPart(ByteBuffer buffer) {
			if (this.buffer.size() <= buffer.remaining()) {
				if (resizeMessage != null)
					MorphUtils.LOGGER.info(resizeMessage.apply(buffer.remaining()));
				this.createBuffer(buffer.remaining() + 32);
			}
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer, buffer, 0);
		}
	}
}
