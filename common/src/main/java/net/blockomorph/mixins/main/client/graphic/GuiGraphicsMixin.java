package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.accessors.GuiGraphicsAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin implements GuiGraphicsAccessor {
	@Shadow @Final private ItemStackRenderState scratchItemStackRenderState;

	@Shadow @Final private MultiBufferSource.BufferSource bufferSource;

	@Override
	public ItemStackRenderState getItemStackRenderState$blockomorph() {
		return this.scratchItemStackRenderState;
	}

	@Override
	public MultiBufferSource.BufferSource getBuffer$blockomorph() {
		return this.bufferSource;
	}
}
