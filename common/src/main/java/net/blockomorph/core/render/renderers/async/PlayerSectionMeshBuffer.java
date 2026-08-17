package net.blockomorph.core.render.renderers.async;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import oshi.annotation.concurrent.NotThreadSafe;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@NotThreadSafe
public class PlayerSectionMeshBuffer implements AutoCloseable {
	private final AutoResizeGpuBuffer vertexBuffer;
	private final AutoResizeGpuBuffer indexBuffer; boolean supportIndexes;
	@Nullable private final IntFunction<String> resizeMessage;
	private final PlayerSectionLayer layer;
	private final int arrayObjectId;
	private MeshData.DrawState meshState;

	public PlayerSectionMeshBuffer(PlayerSectionLayer layer, Function<String, String> ignoredBufName, @Nullable IntFunction<String> resizeMessage) {
		this.arrayObjectId = GlStateManager._glGenVertexArrays();
		this.glBind();
		this.vertexBuffer = new AutoResizeGpuBuffer(BufferType.VERTICES, layer.baseBufferSize());
		this.indexBuffer = new AutoResizeGpuBuffer(BufferType.INDICES, layer.baseBufferSize());
		this.glUnbind();
		this.layer = layer;
		this.resizeMessage = resizeMessage;
	}

	public static void drawOnGpu(Iterable<PlayerSectionMeshBuffer> buffers, RenderTarget renderTarget, Vector3f cameraOffsetMatrix, Supplier<String> ignored) {
		for (PlayerSectionMeshBuffer buffer : buffers) {
			if (!buffer.isReadyToRender()) continue;
			buffer.layer.pipeline().setupRenderState();
			renderTarget.bindWrite(false);
			CompiledShaderProgram shader = RenderSystem.getShader();
			if (shader != null) {
				shader.setDefaultUniforms(VertexFormat.Mode.QUADS, RenderSystem.getModelViewStack(), RenderSystem.getProjectionMatrix(), GuiUtils.MC.getWindow());
				shader.apply();
				Uniform modelOffset = shader.MODEL_OFFSET;
				if (modelOffset != null) {
					modelOffset.set(cameraOffsetMatrix);
					modelOffset.upload();
				}
				buffer.glBind();
				MeshData.DrawState meshState = buffer.meshState;
				VertexFormat.IndexType indexType = meshState.indexType();
				if (!buffer.supportIndexes) {
					RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
					autoIndices.bind(meshState.indexCount());
					indexType = autoIndices.type();
				}
				RenderSystem.drawElements(meshState.mode().asGLMode, meshState.indexCount(), indexType.asGLType);
				if (modelOffset != null) {
					modelOffset.set(0f, 0f, 0f);
				}
				buffer.glUnbind();
				shader.clear();
			}
			buffer.layer.pipeline().clearRenderState();
			GuiUtils.MC.getMainRenderTarget().bindWrite(false);
		}
	}

	public boolean loadMeshData(MeshData meshData) {
		if (meshData == null) {
			this.meshState = null;
			return false;
		}
		this.glBind();
		this.vertexBuffer.uploadMeshDataPart(meshData.vertexBuffer());
		var index = meshData.indexBuffer();
		this.supportIndexes = index != null;
		if (this.supportIndexes)
			this.indexBuffer.uploadMeshDataPart(index);
		this.meshState = meshData.drawState();
		this.glUnbind();
		return true;
	}

	public void loadIndexes(ByteBufferBuilder.Result byteBuffer) {
		this.glBind();
		this.indexBuffer.uploadMeshDataPart(byteBuffer.byteBuffer());
		this.glUnbind();
	}

	public boolean isReadyToRender() {
		return this.meshState != null;
	}

	@Override
	public void close() {
		this.vertexBuffer.buffer.close();
		this.indexBuffer.buffer.close();
		RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
	}

	private void glBind() {
		GlStateManager._glBindVertexArray(this.arrayObjectId);
	}

	private void glUnbind() {
		GlStateManager._glBindVertexArray(0);
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
			this.buffer = new GpuBuffer(this.usage, BufferUsage.DYNAMIC_WRITE, capacity);
			this.buffer.bind();
			if (this.usage == BufferType.VERTICES) {
				DefaultVertexFormat.BLOCK.setupBufferState();
			}
		}

		void uploadMeshDataPart(ByteBuffer buffer) {
			if (this.buffer.size <= buffer.remaining()) {
				if (resizeMessage != null)
					MorphUtils.LOGGER.info(resizeMessage.apply(buffer.remaining()));
				this.createBuffer(buffer.remaining() + 32);
			}
			this.buffer.bind();
			this.buffer.write(buffer, 0);
		}
	}
}
