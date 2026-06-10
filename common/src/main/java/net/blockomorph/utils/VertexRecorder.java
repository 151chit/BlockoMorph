package net.blockomorph.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class VertexRecorder implements VertexConsumer {
    private final List<Consumer<VertexConsumer>> commands = new ArrayList<>();

    public void replay(VertexConsumer realBuffer) {
        for (Consumer<VertexConsumer> command : this.commands) {
            command.accept(realBuffer);
        }
        this.commands.clear();
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.commands.add(buffer -> buffer.addVertex(x, y, z));
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.commands.add(buffer -> buffer.setColor(r, g, b, a));
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.commands.add(buffer -> buffer.setColor(color));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.commands.add(buffer -> buffer.setUv(u, v));
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.commands.add(buffer -> buffer.setUv1(u, v));
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.commands.add(buffer -> buffer.setUv2(u, v));
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.commands.add(buffer -> buffer.setNormal(x, y, z));
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.commands.add(buffer -> buffer.setLineWidth(width));
        return this;
    }
}