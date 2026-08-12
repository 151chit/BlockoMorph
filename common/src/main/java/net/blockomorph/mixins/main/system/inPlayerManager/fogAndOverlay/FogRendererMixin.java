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
	private Vector4f handle(Camera camera, int i, boolean bl, DeltaTracker deltaTracker, float f, ClientLevel clientLevel, Operation<Vector4f> original) {
		var block = BlockInCamera.findFluid(camera, clientLevel);
		if (block != null) {
			LevelWithFlags.of(clientLevel).flags().overrideBlockGetter = (ignored1, ignoredX, ignoredY, ignoredZ) -> block.getBlockState();
		}
		try {
			return original.call(camera, i, bl, deltaTracker, f, clientLevel);
		} finally {
			LevelWithFlags.of(clientLevel).flags().overrideBlockGetter = null;
		}
	}
}
