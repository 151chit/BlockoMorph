package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsAcc implements Accessors.GuiGraphicsAccessor {
	@Shadow @Final private GuiRenderState guiRenderState;
	@Override
	public GuiRenderState getGuiRenderState$bm() {
		return this.guiRenderState;
	}
}
