package net.blockomorph.screens;

import net.minecraft.client.gui.components.EditBox;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

public class ListenerEditBox extends EditBox {
    private Consumer<String> run;
    protected boolean edit = true;
    
	public ListenerEditBox(Font font, int x, int y, int weight, int height, Component name, Consumer<String> run) {
      super(font, x, y, weight, height, name);
      this.run = run;
    }

    public boolean keyPressed(int key, int b, int c) {
    	if (this.active) {
            boolean flag = super.keyPressed(key, b, c);
      	    if (this.edit) this.run.accept(this.getValue());
      	    return flag;
    	}
    	return false;
    }

    public boolean charTyped(char c, int type) {
    	if (this.active) {
      	    boolean flag = super.charTyped(c, type);
      	    if (this.edit) this.run.accept(this.getValue());
      	    return flag;
    	}
    	return false;
    }

    public void setEditable(boolean e) {
        super.setEditable(e);
        this.edit = e;
    }

    public void renderWidget(GuiGraphics g, int x, int y, float ticks) {
    	if (this.active)
            super.renderWidget(g, x, y, ticks);
    }

}
