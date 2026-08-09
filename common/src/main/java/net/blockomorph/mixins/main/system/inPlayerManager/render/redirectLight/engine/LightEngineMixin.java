package net.blockomorph.mixins.main.system.inPlayerManager.render.redirectLight.engine;

import net.blockomorph.utils.side.Side;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.mixin.PrimitiveCancelSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LightEngine.class, priority = 999) //for override forPoints
public class LightEngineMixin {
	@Shadow @Final protected LightChunkGetter chunkSource;

	@FastInject(method = "getLightValue", at = @At("HEAD"), continueIf = @PrimitiveCancelSignal(intValue = -1))
	private int check(BlockPos pos) {
		var provider = LevelWithFlags.of(this.chunkSource.getLevel()).flags().customLightProvider;
		if (provider != null && provider.side() == Side.get()) {
			LightLayer layer = null;
			if (this.getThis() instanceof BlockLightEngine) layer = LightLayer.BLOCK;
			if (this.getThis() instanceof SkyLightEngine) layer = LightLayer.SKY;
			return provider.getBrightness(pos, layer);
		}
		return -1;
	}

	@Unique
	private LightEngine<?, ?> getThis() {
		return (LightEngine<?, ?>) (Object)this;
	}
}
