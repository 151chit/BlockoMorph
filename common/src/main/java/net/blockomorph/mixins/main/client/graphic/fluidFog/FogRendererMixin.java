package net.blockomorph.mixins.main.client.graphic.fluidFog;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Shadow private static float fogRed;
	@Shadow private static float fogGreen;
	@Shadow private static float fogBlue;
	@Unique private static final FogLiquidModifier LIQUID_MODIFIER = new FogLiquidModifier();

	@Inject(method = "setupColor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V", ordinal = 1, remap = false))
	private static void setCol(Camera camera, float f, ClientLevel clientLevel, int i, float g, CallbackInfo ci) {
		FogLiquidModifier.LiquidFogData fogData = LIQUID_MODIFIER.getFog(GuiUtils.MC.level, false);
		if (fogData == null) return;
		int color = fogData.color();
		fogRed = (float) FastColor.ARGB32.red(color) / 255;
		fogGreen = (float) FastColor.ARGB32.green(color) / 255;
		fogBlue = (float) FastColor.ARGB32.blue(color) / 255;
	}

	@Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V", remap = false), cancellable = true)
	private static void setF(Camera camera, FogRenderer.FogMode fogMode, float f, boolean bl, float g, CallbackInfo ci) {
		FogLiquidModifier.LiquidFogData fogData = LIQUID_MODIFIER.getFog(GuiUtils.MC.level, false);
		if (fogData == null) return;
		FogShape shape = FogShape.SPHERE;
		float end = fogData.end();
		if (FastColor.ARGB32.alpha(FastColor.ARGB32.alpha(fogData.color())) <= 235) {
			int renderDistance = GuiUtils.MC.options.getEffectiveRenderDistance();
			if (fogData.end() > renderDistance) {
				shape = FogShape.CYLINDER;
				end = renderDistance;
			}
		}
		RenderSystem.setShaderFogStart(fogData.start());
		RenderSystem.setShaderFogEnd(end);
		RenderSystem.setShaderFogShape(shape);
		ci.cancel();
	}
}
