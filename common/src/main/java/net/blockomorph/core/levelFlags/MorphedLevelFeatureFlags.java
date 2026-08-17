package net.blockomorph.core.levelFlags;

import net.blockomorph.utils.side.Side;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class MorphedLevelFeatureFlags {
	public boolean morphedBlockGetterDisabled;
	public boolean redstoneAlwaysOn; //server, main thread, tnt spawn direct
	public boolean flywheelDisabled; //client, main thread
	public BlockStateGetter overrideBlockGetter; //fog
	public LightProvider customLightProvider;

	public interface LightProvider {
		LightProvider ALWAYS_LIGHT = new LightProvider() {
			@Override public int getMaxBrightness(BlockPos pos) { return 15; }
			@Override public int getBrightness(BlockPos pos, LightLayer layer) { return 15; }
			@Override public int earlyRenderLightBeforeNormalize(BlockPos pos) { return LightTexture.FULL_BRIGHT; }
			@Override public Side side() { return Side.CLIENT; }
		};
		int getMaxBrightness(BlockPos pos); //terrain or sky = max (0/15)
		int getBrightness(BlockPos pos, LightLayer layer); //(0/15)
		int earlyRenderLightBeforeNormalize(BlockPos pos); //(packed coords LightTexture)
		Side side();
	}

	public interface BlockStateGetter {
		BlockState getBlockState(BlockGetter blockGetter, int x, int y, int z);
	}
}
