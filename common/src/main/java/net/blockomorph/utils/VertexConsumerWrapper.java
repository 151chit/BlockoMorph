package net.blockomorph.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;

public abstract class VertexConsumerWrapper implements VertexConsumer {
	protected final VertexConsumer real;

	protected VertexConsumerWrapper(VertexConsumer real) {
		this.real = real;
	}

	@Override
	public VertexConsumer setColor(int rgba) {
		this.real.setColor(rgba);
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		this.real.setLineWidth(width);
		return this;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		this.real.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		this.real.setColor(r, g, b, a);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.real.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		this.real.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		this.real.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		this.real.setNormal(x, y, z);
		return this;
	}
}
