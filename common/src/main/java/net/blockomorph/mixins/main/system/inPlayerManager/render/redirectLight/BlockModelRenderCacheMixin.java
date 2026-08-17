package net.blockomorph.mixins.main.system.inPlayerManager.render.redirectLight;

import net.blockomorph.utils.side.Side;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.mixin.PrimitiveCancelSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$Cache")
public class BlockModelRenderCacheMixin {

	@FastInject(method = "getLightColor", at = @At("HEAD"), continueIf = @PrimitiveCancelSignal(intValue = -1))
	private int check(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		var provider = LevelWithFlags.of(level).flags().customLightProvider;
		if (provider != null && provider.side() == Side.get()) {
			return provider.earlyRenderLightBeforeNormalize(pos);
		}
		return -1;
	}
}
