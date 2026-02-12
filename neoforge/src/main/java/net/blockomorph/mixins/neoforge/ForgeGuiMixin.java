package net.blockomorph.mixins.neoforge;

import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeGui.class)
public class ForgeGuiMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void render(GuiGraphics guiGraphics, float delta, CallbackInfo ci) {
		GuiUtils.renderOverlay(guiGraphics, delta);
	}
}
