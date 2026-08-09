package net.blockomorph.mixins.main.system.inPlayerManager.fogAndOverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.BlockInCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

	@WrapMethod(method = "setupFog")
	private Vector4f handle(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, Operation<Vector4f> original) {
		var block = BlockInCamera.findFluid(camera, level);
		if (block != null) {
			LevelWithFlags.of(level).flags().overrideBlockGetter = (ignored1, ignoredX, ignoredY, ignoredZ) -> block.getBlockState();
		}
		try {
			return original.call(camera, renderDistanceInChunks, deltaTracker, darkenWorldAmount, level);
		} finally {
			LevelWithFlags.of(level).flags().overrideBlockGetter = null;
		}
	}
}
