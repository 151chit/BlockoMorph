package net.blockomorph.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;

public abstract class VertexConsumerWrapper implements VertexConsumer {
	protected final VertexConsumer real;

	protected VertexConsumerWrapper(VertexConsumer real) {
		this.real = real;
	}

	@Override
	public VertexConsumer vertex(double x, double y, double z) {
		this.real.vertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer color(int r, int g, int b, int a) {
		this.real.color(r, g, b, a);
		return this;
	}

	@Override
	public VertexConsumer uv(float u, float v) {
		this.real.uv(u, v);
		return this;
	}

	@Override
	public VertexConsumer overlayCoords(int x, int y) {
		this.real.overlayCoords(x, y);
		return this;
	}

	@Override
	public VertexConsumer uv2(int u, int v) {
		this.real.uv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		this.real.normal(x, y, z);
		return this;
	}

	@Override
	public void endVertex() {
		this.real.endVertex();
	}

	@Override
	public void defaultColor(int r, int g, int b, int a) {
		this.real.defaultColor(r, g, b, a);
	}

	@Override
	public void unsetDefaultColor() {
		this.real.unsetDefaultColor();
	}
}
