package net.blockomorph.utils.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.SortedSet;

public interface LevelRendererAccessor {
	Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks();
	void prepareTranslucentPlayersForRender$blockomorph(PoseStack stack, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profilerFiller);

	static LevelRendererAccessor of(Object lr) {
		return (LevelRendererAccessor) lr;
	}
}
