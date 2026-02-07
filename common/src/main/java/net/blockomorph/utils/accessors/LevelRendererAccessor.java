package net.blockomorph.utils.accessors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;

import java.util.SortedSet;

public interface LevelRendererAccessor {
	Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks();
	void prepareTranslucentPlayersForSubmit$blockomorph(LevelRenderState levelRenderState);

	static LevelRendererAccessor of(Object lr) {
		return (LevelRendererAccessor) lr;
	}
}
