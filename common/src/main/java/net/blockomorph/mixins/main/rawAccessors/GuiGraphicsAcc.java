package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphics.class)
public class GuiGraphicsAcc implements Accessors.GuiGraphicsAccessor {
	@Shadow @Final GuiRenderState guiRenderState;

	@Override
	public GuiRenderState getGuiRenderState$bm() {
		return this.guiRenderState;
	}
}
