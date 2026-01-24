package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.screens.utils.NoShadowFormattedCharSequence;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public class FontMixin {

	@Inject(at = @At("HEAD"), method = "renderText(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F", cancellable = true)
	private void renderText(FormattedCharSequence formattedCharSequence, float f, float g, int i, boolean bl, Matrix4f matrix4f, MultiBufferSource multiBufferSource, Font.DisplayMode displayMode, int j, int k, CallbackInfoReturnable<Float> cir) {
		if (formattedCharSequence instanceof NoShadowFormattedCharSequence) {
			if (bl) {
				cir.setReturnValue(0f);
			} else {
				matrix4f.translate(new Vector3f(0.0F, 0.0F, -0.03F));
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", cancellable = true)
	private void drawText(FormattedCharSequence formattedCharSequence, float f, float g, int i, boolean bl, Matrix4f matrix4f, MultiBufferSource multiBufferSource, Font.DisplayMode displayMode, int j, int k, CallbackInfoReturnable<Integer> cir) {
		if (formattedCharSequence instanceof NoShadowFormattedCharSequence && bl)
			cir.setReturnValue(cir.getReturnValue());
	}
}
