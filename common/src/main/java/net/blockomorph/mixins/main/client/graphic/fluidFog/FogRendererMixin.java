package net.blockomorph.mixins.main.client.graphic.fluidFog;

import com.mojang.blaze3d.shaders.FogShape;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.ARGB;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	private static final FogLiquidModifier LIQUID_MODIFIER = new FogLiquidModifier();

	@Inject(method = "setupFog", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
	private static void changeFog(Camera camera, FogRenderer.FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> cir) {
		FogLiquidModifier.LiquidFogData fogData = LIQUID_MODIFIER.getFog(GuiUtils.MC.level.entitiesForRendering(), false);
		if (fogData == null) return;
		FogShape shape = FogShape.SPHERE;
		int color = fogData.color();
		float end = fogData.end();
		if (ARGB.alpha(ARGB.alpha(color)) <= 235) {
			int renderDistance = GuiUtils.MC.options.getEffectiveRenderDistance();
			if (fogData.end() > renderDistance) {
				shape = FogShape.CYLINDER;
				end = renderDistance;
			}
		}
		FogParameters fogParameters = new FogParameters(fogData.start(), end, shape, ARGB.red(color), ARGB.green(color), ARGB.blue(color), 1);
		cir.setReturnValue(fogParameters);
	}
}
