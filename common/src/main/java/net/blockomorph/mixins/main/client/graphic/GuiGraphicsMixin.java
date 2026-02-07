package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.accessors.GuiGraphicsAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin implements GuiGraphicsAccessor {
	@Shadow @Final GuiRenderState guiRenderState;

	@Override
	public GuiRenderState getGuiState$blockomorph() {
		return this.guiRenderState;
	}
}
