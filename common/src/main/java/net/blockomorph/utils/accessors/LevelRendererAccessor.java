package net.blockomorph.utils.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.SortedSet;

public interface LevelRendererAccessor {
	void renderBlockHitbox(PoseStack p_109783_, VertexConsumer p_109784_, VoxelShape p_109785_, double p_109786_, double p_109787_, double p_109788_, float p_109789_, float p_109790_, float p_109791_, float p_109792_);

	Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks();

	static LevelRendererAccessor of(LevelRenderer lr) {
		return (LevelRendererAccessor) lr;
	}
}
