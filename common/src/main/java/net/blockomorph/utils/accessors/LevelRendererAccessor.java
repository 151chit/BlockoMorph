package net.blockomorph.utils.accessors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;

import java.util.SortedSet;

public interface LevelRendererAccessor {
	Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks();

	static LevelRendererAccessor of(Object lr) {
		return (LevelRendererAccessor) lr;
	}
}
