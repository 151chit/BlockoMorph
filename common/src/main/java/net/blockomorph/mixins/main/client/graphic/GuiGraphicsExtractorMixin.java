package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.accessors.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorAccessor {
	@Shadow @Final private GuiRenderState guiRenderState;

	@Override
	public GuiRenderState getGuiState$blockomorph() {
		return this.guiRenderState;
	}
}
