package net.blockomorph.utils.accessors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;

import java.util.SortedSet;

@Environment(EnvType.CLIENT)
public interface LevelRendererAccessor {
	Long2ObjectMap<SortedSet<BlockDestructionProgress>> getBrakingBlocks();

	static LevelRendererAccessor of(LevelRenderer lr) {
		return (LevelRendererAccessor) lr;
	}
}
