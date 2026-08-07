package net.blockomorph.core.render.blockGetter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ProxyLightEngine extends LevelLightEngine {
	private final ProxyLightListener block;
	private final ProxyLightListener sky;

	ProxyLightEngine(InPlayerBlockAndTintGetter blockGetter) {
		super(new LightChunkGetter() {
			@Override public LightChunk getChunkForLighting(int x, int z) { return null; }
			@Override public BlockGetter getLevel() { return blockGetter; }
		}, false, false);
		this.block = new ProxyLightListener(LightLayer.BLOCK, blockGetter);
		this.sky = new ProxyLightListener(LightLayer.SKY, blockGetter);
	}

	private record ProxyLightListener(LightLayer layer, InPlayerBlockAndTintGetter owner) implements LayerLightEventListener {
		@Override
		public int getLightValue(BlockPos blockPos) {
			return this.owner.getRealWorld().getLightEngine().getLayerListener(this.layer).getLightValue(this.owner.translateToReal(blockPos));
		}

		@Override @Nullable public DataLayer getDataLayerData(SectionPos pos) { return null; }
		@Override public void checkBlock(BlockPos pos) {}
		@Override public boolean hasLightWork() { return false; }
		@Override public int runLightUpdates() { return 0; }
		@Override public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {}
		@Override public void setLightEnabled(ChunkPos pos, boolean enable) {}
		@Override public void propagateLightSources(ChunkPos pos) {}
	}

	@Override
	public LayerLightEventListener getLayerListener(LightLayer layer) {
		return switch (layer) {
			case BLOCK -> this.block;
			case SKY -> this.sky;
		};
	}

	@Override
	public int getRawBrightness(BlockPos pos, int skyDampen) {
		int skyLight = this.getLayerListener(LightLayer.SKY).getLightValue(pos) - skyDampen;
		int blockLight = this.getLayerListener(LightLayer.BLOCK).getLightValue(pos);
		return Math.max(blockLight, skyLight);
	}
}