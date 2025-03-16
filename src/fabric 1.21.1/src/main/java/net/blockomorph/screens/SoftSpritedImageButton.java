package net.blockomorph.screens;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Button;

public class SoftSpritedImageButton extends ImageButton {
    public SoftSpritedImageButton(int i, int j, int k, int l, WidgetSprites widgetSprites, Button.OnPress onPress) {
    	super(i, j, k, l, widgetSprites, onPress);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        ResourceLocation resourceLocation = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
        guiGraphics.blit(resourceLocation, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
    }
}
