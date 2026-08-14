package net.blockomorph.mixins.main.system.inPlayerManager.fogAndOverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.Level;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

	@WrapMethod(method = "setupFog")
	private static FogParameters handle(Camera camera, FogRenderer.FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, Operation<FogParameters> original) {
		return checkMorphedFog(camera, GuiUtils.MC.level, () -> original.call(camera, fogMode, vector4f, f, bl, g));
	}

	@WrapMethod(method = "computeFogColor")
	private static Vector4f handleColor(Camera camera, float f, ClientLevel clientLevel, int i, float g, Operation<Vector4f> original) {
		return checkMorphedFog(camera, clientLevel, () -> original.call(camera, f, clientLevel, i, g));
	}

	@Unique
	private static <T> T checkMorphedFog(Camera camera, Level clientLevel, Supplier<T> operation) {
		var block = BlockInCamera.findFluid(camera, clientLevel);
		if (block != null) {
			LevelWithFlags.of(clientLevel).flags().overrideBlockGetter = (ignored1, ignoredX, ignoredY, ignoredZ) -> block.getBlockState();
		}
		try {
			return operation.get();
		} finally {
			LevelWithFlags.of(clientLevel).flags().overrideBlockGetter = null;
		}
	}
}
