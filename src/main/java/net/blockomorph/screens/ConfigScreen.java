package net.blockomorph.screens;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.config.*;
import net.blockomorph.network.ServerBoundConfigUpdatePacket;


import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.util.List;
import java.util.Arrays;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.renderer.RenderType;

public class ConfigScreen extends Screen {
   private static final ResourceLocation texture = ResourceLocation.tryParse("blockomorph:textures/screens/config_screen.png");
   private static final ResourceLocation CONF = ResourceLocation.tryParse("blockomorph:textures/screens/configs.png");
   private static final ResourceLocation BUTS = ResourceLocation.tryParse("blockomorph:textures/screens/list_but.png");
   private final Level world;
   private final Player entity;
   protected int imageWidth = 176;
   protected int imageHeight = 166;
   protected int leftPos;
   protected int topPos;
   private EnumConfig enumList;
   private int enumListOffset; //in this version static 0

   public ConfigScreen() {
   	   super(Component.literal("config_screen"));
   	   this.world = Minecraft.getInstance().level;
	   this.entity = Minecraft.getInstance().player;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
   	    super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
		ConfigInstance<?> op = this.getProp(mouseX, mouseY);
		if (op != null) {
			String name = Component.translatable("gui.blockomorph." + op.getName()).getString();
			if (op.getTooltip() != null) {
				guiGraphics.renderTooltip(this.font, op.getTooltip(), mouseX, mouseY);
			} else if (name.length() > 13) {
		        guiGraphics.renderTooltip(this.font, Component.literal(name), mouseX, mouseY);
			}
		}
   }

   @Override
   public void init() {
		super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(RenderType::guiTextured, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		this.renderConfigs(guiGraphics, gx, gy);
		guiGraphics.drawString(this.font, Component.translatable("menu.options"), this.leftPos + 8, this.topPos + 6, 4210752, false);
		//scroller always locked in this mod version
		guiGraphics.blitSprite(RenderType::guiTextured, ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled"), this.leftPos + 158, this.topPos + 16, 12, 15);
		RenderSystem.disableBlend();
   }

   public ConfigInstance<?> getProp(double x, double y) {
   	    if (x < this.leftPos + 10 || x > this.leftPos + 153 || y < this.topPos + 15 || y > this.topPos + 154) return null;
   	    for (int i = 0; i < Config.getInstance().options.size() - 1; i++) { //max 7
   	    	if (y > this.topPos + 15 + i * 20 && y < this.topPos + 15 + i * 20 + 20) {
   	    		return Config.getInstance().options.get(i);
   	    	}
   	    }
   	    return null;
   }

   public boolean mouseClicked(double gx, double gy, int type) {
   	    if (type == 0) {
   	    	ConfigInstance<?> op = this.getProp(gx, gy);
   	    	boolean flag = this.enumClick(gx, gy);
   	    	this.enumList = null;
   	    	if (flag) return super.mouseClicked(gx, gy, type);
   	    	if (op instanceof BooleanConfig e) {
   	    		this.send(new ServerBoundConfigUpdatePacket(op.getName(), e.setValue(!e.getValue()) + "" ));
   	    		this.playDownSound();
   	    	} else if (op instanceof ListConfig e2) {
   	    		int x = this.leftPos + 10;
   	    		int y = this.topPos + 15 + Config.getInstance().options.indexOf(op) * 20;
   	    		if (gx > x + 125 && gx < x + 125 + 16 && gy > y + 2 && gy < y + 2 + 16) {
   	    			this.playDownSound();
   	    			if (e2.getName().equals("allowedBlocks")) {
   	    			    this.minecraft.setScreen(new MorphScreen(Config.Mode.WHITELIST, true));
   	    			} else if (e2.getName().equals("bannedBlocks")) {
   	    				this.minecraft.setScreen(new MorphScreen(Config.Mode.BLACKLIST, true));
   	    			}
   	    		}
   	    	} else if (op instanceof EnumConfig e3) {
   	    		this.enumList = e3;
   	    		this.playDownSound();
   	    	}
   	    }
   	    return super.mouseClicked(gx, gy, type);
   }

   private void send(ServerBoundConfigUpdatePacket p) {
   	    MorphUtils.sendServer(p);
   }

   private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + height;
   }

   public boolean isPauseScreen() {
        return false;
   }

   private void playDownSound() {
   	    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private void renderConfigs(GuiGraphics guiGraphics, int gx, int gy) {
   	    int i = 0;
   	    for (ConfigInstance<?> op : Config.getInstance().options) {
   	    	if (i > 6) break;
   	    	if (op.getName().equals("canOperatorModifyConfig")) continue;
   	    	if (op instanceof BooleanConfig e) {
   	    		guiGraphics.blit(RenderType::guiTextured, CONF, this.leftPos + 10 , this.topPos + 15 + i * 20, 0, 0, 144, 20, 144, 74);
   	    		if (e.getValue()) guiGraphics.blit(RenderType::guiTextured, CONF, this.leftPos + 10 + 105, this.topPos + 15 + i * 20 + 3, 0, 60, 24, 14, 144, 74);
   	    	} else if (op instanceof EnumConfig e2) {
   	    		guiGraphics.blit(RenderType::guiTextured, CONF, this.leftPos + 10 , this.topPos + 15 + i * 20, 0, 20, 144, 20, 144, 74);
   	    		String value = e2.getValue().toString().toLowerCase();
   	    	    if (value.length() > 8) {
   	    	    	value = value.substring(0, 7);
   	    	    	value = value + "..";
   	    	    }
   	    	    guiGraphics.drawString(this.font, value, this.leftPos + 97 + 10, this.topPos + 15 + i * 20 + 6, -12821534, false); //render enum value
   	    	} else if (op instanceof ListConfig e3) {
   	    		int x = this.leftPos + 10;
   	    		int y = this.topPos + 15 + i * 20;
   	    		guiGraphics.blit(RenderType::guiTextured, CONF, x, y, 0, 40, 144, 20, 144, 74);
   	    		if (gx > x + 125 && gx < x + 125 + 16 && gy > y + 2 && gy < y + 2 + 16) {
   	    			guiGraphics.blit(RenderType::guiTextured, BUTS, x + 125, y + 2, 0, 16, 16, 16, 16, 32);
   	    		} else {
   	    			guiGraphics.blit(RenderType::guiTextured, BUTS, x + 125, y + 2, 0, 0, 16, 16, 16, 32);
   	    		}
   	    	}
   	    	String name = Component.translatable("gui.blockomorph." + op.getName()).getString();
   	    	if (name.length() > 14) {
   	    	    name = name.substring(0, 13);
   	    	    name = name + "..";
   	        }
   	    	guiGraphics.drawString(this.font, name, this.leftPos + 10 + 4, this.topPos + 15 + i * 20 + 5, op instanceof ListConfig ? -6710887 : -1, false);
   	    	i++;
   	    }
   	    if (this.enumList != null) this.renderEnumList(guiGraphics, gx, gy);
   }

   private void renderEnumList(GuiGraphics guiGraphics, int mouseX, int mouseY) { //new
   	    List<Enum<?>> vals = Arrays.asList(enumList.getAllEnumValues());
        int maxWidth = this.getLongWord(vals);
        int posY = this.topPos + 15 + (Config.getInstance().options.indexOf(this.enumList)) * 20 + 14;
        int height = 12 * Math.min(vals.size(), 7);
        int weidth = this.leftPos + 97 + 10;
   	    guiGraphics.fill(weidth, posY, weidth + maxWidth + 4, posY + height, Integer.MIN_VALUE);
   	    int textX;
        int textY = 0;
   	    for (int i = 0; i < 7 && i < vals.size(); i++) { // i < vals.size()
            String string = vals.get(i + this.enumListOffset).toString().toLowerCase();
            textX = weidth + 2;
            textY = posY + 2 + i * 12;

            if (isMouseOver(mouseX, mouseY, textX, textY - 2, maxWidth, 12)) {
                guiGraphics.drawString(this.font, string, textX, textY, 0xFFFF00FF);
            } else {
                guiGraphics.drawString(this.font, string, textX, textY, -1);
            }
        }
        maxWidth = maxWidth + 4;
        if (vals.size() > 6 && this.enumListOffset + 7 < vals.size()) {
        	textY = textY + 9;
        	for (int i1 = 0; i1 < maxWidth; i1++) {
                if (i1 % 2 == 0) {
                   guiGraphics.fill(
                   weidth + i1,
                   textY,
                   weidth + i1 + 1,
                   textY + 1,
                   -1
                   );
                }
            }
        }
        if (this.enumListOffset > 0) {
            for (int k = 0; k < maxWidth; k++) {
                if (k % 2 == 0) {
                   guiGraphics.fill(weidth + k, posY, weidth + k + 1, posY + 1, -1);
                }
            }
       }
   }

   private boolean enumClick(double mouseX, double mouseY) {
   	    if (this.enumList != null) {
   	        List<Enum<?>> vals = Arrays.asList(enumList.getAllEnumValues());
            int maxWidth = this.getLongWord(vals);
            int posY = this.topPos + 15 + (Config.getInstance().options.indexOf(this.enumList)) * 20 + 14;
            int count = Math.min(vals.size(), 7);
            int height = 12 * count;
            int weidth = this.leftPos + 97 + 10;
   	        for (int i = 0; i < count; i++) {
                String string = vals.get(i + this.enumListOffset) + "";
                int textX = weidth + 2;
                int textY = posY + 2 + i * 12;

                if (isMouseOver(mouseX, mouseY, textX, textY - 2, maxWidth, 12)) {
                	this.send(new ServerBoundConfigUpdatePacket(this.enumList.getName(), string));
                	this.playDownSound();
                    return true;
                }
            }
   	    }
   	    return false;
   }

   private int getLongWord(List<Enum<?>> values) {
   	    int maxWidth = 0;
   	    for (Enum<?> enumo : values) {
        	 String string = enumo.toString().toLowerCase();
             int stringWidth = this.font.width(string); 
             if (stringWidth > maxWidth) {
                 maxWidth = stringWidth;
             }
        }
        return maxWidth;
   }
}
