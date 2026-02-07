package net.blockomorph.mixins.main.client.graphic.fluidFog;

import net.blockomorph.screens.utils.ModLiquidFogEnvironment;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

	@Shadow @Final private static List<FogEnvironment> FOG_ENVIRONMENTS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void clinit(CallbackInfo ci) {
		FOG_ENVIRONMENTS.addFirst(new ModLiquidFogEnvironment());
	}
}
