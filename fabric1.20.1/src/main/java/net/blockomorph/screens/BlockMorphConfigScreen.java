package net.blockomorph.screens;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.ServerBoundBlockMorphPacket;
import net.blockomorph.screens.MorphScreen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.gui.components.EditBox;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;

import java.util.Optional;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

public class BlockMorphConfigScreen extends Screen {
   private static final ResourceLocation texture = new ResourceLocation("blockomorph:textures/screens/morph_config_gui.png");
   private static final ResourceLocation PROP = new ResourceLocation("blockomorph:textures/screens/properties.png");
   private static final ResourceLocation PACKET_ID = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_block_morph_packet");
   private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
   private final SoundManager sound = Minecraft.getInstance().getSoundManager();
   private BlockState playerState = Blocks.AIR.defaultBlockState();
   private CompoundTag playerTag = new CompoundTag();
   private static final BlockPos AIR = new BlockPos(0, 512, 0); 
   private final Level world;
   private final Player entity;
   public String tagException = "";
   public boolean init;
   protected int imageWidth = 176;
   protected int imageHeight = 166;
   protected int leftPos;
   protected int topPos;
   private int propOff;
   private int listPropNumber = -1;
   private EnumProperty listProp;
   EditBox tagsBox;

   public BlockMorphConfigScreen(Component text) {
   	   super(text);
   	   this.world = Minecraft.getInstance().level;
	   this.entity = Minecraft.getInstance().player;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderLb(guiGraphics);
		tagsBox.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderBlockAsIcon(guiGraphics, partialTicks);
	    String name = playerState.getBlock().getName().getString();
		if (name.length() > 13 && mouseX > this.leftPos + 15 && mouseX < this.leftPos + 75 && mouseY > this.topPos + 18 && mouseY < this.topPos + 78) guiGraphics.renderTooltip(this.font, Component.literal(name), mouseX, mouseY);
		if (!tagException.isEmpty()) {
		    guiGraphics.fill(this.leftPos, this.topPos + this.imageHeight - 2, this.leftPos + this.font.width(tagException), this.topPos + this.imageHeight + 14, Integer.MIN_VALUE);
            guiGraphics.drawString(this.font, tagException, this.leftPos, this.topPos + this.imageHeight + 2, 16733525);
		}
		Property<?> prop = this.getProp(mouseX, mouseY, false);
		if (prop instanceof EnumProperty enumprop && mouseX > this.leftPos + 93 + 35) {
			String value = (this.playerState.getValue(prop)).toString().toLowerCase();
			if (value.length() > 4) {
		        guiGraphics.renderTooltip(this.font, Component.literal(value), mouseX, mouseY);
			} else {
				guiGraphics.renderTooltip(this.font, Component.literal(prop.getName()), mouseX, mouseY);
			}
		} else if (prop != null) {
			guiGraphics.renderTooltip(this.font, Component.literal(prop.getName()), mouseX, mouseY);
		}
   }

   public void morphUpdate(BlockState state) {
   	    CompoundTag tag = ((PlayerAccessor)this.entity).getTag();
   	    this.tagException = "";
   	    if (state.getBlock() instanceof EntityBlock) {
			tagsBox.setFocused(true);
			tagsBox.setEditable(true);
            if (state.getBlock() != this.playerState.getBlock())
			    tagsBox.setValue(tag.toString());
		} else {
			tagsBox.setFocused(false);
			tagsBox.setEditable(false);
			tagsBox.setValue("");
		}
		this.playerState = state;
		this.playerTag = tag;
   }

   public boolean isPauseScreen() {
        return false;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		if (!tagsBox.canConsumeInput()) 
		    guiGraphics.blit(new ResourceLocation("blockomorph:textures/screens/morph_gui_icons.png"), this.leftPos + 7, this.topPos + 139, 0, 0, 162, 19, 162, 19);
		guiGraphics.blit(new ResourceLocation("blockomorph:textures/screens/exit_tabs.png"), this.leftPos + 4, this.topPos - 19, 0, 23, 80, 22, 80, 46);
		this.renderProp(guiGraphics, gx, gy);
		RenderSystem.disableBlend();
   }

   protected void renderLb(GuiGraphics guiGraphics) {
   	    String name = playerState.getBlock().getName().getString();
   	    if (name.length() > 13) { 
   	    	name = name.substring(0, 13);
   	    	name = name + "...";
   	    }
   	    guiGraphics.drawString(this.font, name, this.leftPos + 6, this.topPos + 6, 4210752, false);
   	    guiGraphics.drawString(this.font, "BlockStates", this.leftPos + 100, this.topPos + 15, 4210752, false);
   	    guiGraphics.drawString(this.font, "NBT", this.leftPos + 9, this.topPos + 130, 4210752, false);
   }

   private void renderProp(GuiGraphics guiGraphics, int mouseX, int mouseY) {
   	    Collection<Property<?>> properties = this.playerState.getProperties();
   	    List<Property<?>> props = new ArrayList<>(properties);
   	    int list = -1;
   	    for (int i = 0; i < 5; i++) {
   	    	if (this.propOff + i < props.size()) {
   	    	    Property<?> prop = props.get(this.propOff + i);
   	    	    if (prop instanceof BooleanProperty bool) {
   	    	    	guiGraphics.blit(PROP, this.leftPos + 93 , this.topPos + 24 + i * 19, 0, 38, 67, 19, 67, 64);
   	    	    	if (this.playerState.getValue(bool))
   	    	    	    guiGraphics.blit(PROP, this.leftPos + 93 + 44 , this.topPos + 24 + i * 19 + 6, 0, 57, 15, 7, 67, 64);
   	    	    } else if (prop instanceof IntegerProperty integer) {
   	    	    	guiGraphics.blit(PROP, this.leftPos + 93 , this.topPos + 24 + i * 19, 0, 19, 67, 19, 67, 64);
   	    	    	guiGraphics.drawString(this.font, this.playerState.getValue(integer) + "", this.leftPos + 93 + 42, this.topPos + 24 + i * 19 + 6, -1, false);
   	    	    } else if (prop instanceof EnumProperty enumprop) {
   	    	    	guiGraphics.blit(PROP, this.leftPos + 93 , this.topPos + 24 + i * 19, 0, 0, 67, 19, 67, 64);
   	    	    	String value = (this.playerState.getValue(prop)).toString().toLowerCase();
   	    	    	if (value.length() > 4) {
   	    	    		value = value.substring(0, 3);
   	    	    		value = value + "..";
   	    	    	}
   	    	    	guiGraphics.drawString(this.font, value, this.leftPos + 93 + 42, this.topPos + 24 + i * 19 + 6, -12821534, false); //render enum value
   	    	    	if (this.listProp == prop) {
   	    	            list = i;
   	                }
   	    	    }
   	    	    String name = prop.getName();
   	    	    if (name.length() > 6) {
   	    	    	name = name.substring(0, 5);
   	    	    	name = name + "..";
   	    	    }
   	    	    guiGraphics.drawString(this.font, name + ":", this.leftPos + 93 + 3, this.topPos + 24 + i * 19 + 5, -1, false);
   	    	} else break;
   	    }
   	    if (list != -1 && this.listPropNumber > -1 && this.listPropNumber < 5) {
   	    	this.renderEnumList(guiGraphics, mouseX, mouseY, list);
   	    }
   }

   private void renderEnumList(GuiGraphics guiGraphics, int mouseX, int mouseY, int iC) {
   	    Collection<Enum<?>> values = listProp.getPossibleValues();
   	    List<Enum<?>> vals = new ArrayList<>(values);
        int maxWidth = this.getLongWord(values);
        int posY = this.topPos + 24 + iC * 19 + 6 + 8;
        int height = 12 * values.size();
        int weidth = this.leftPos + 93 + 40;
   	    guiGraphics.fill(weidth, posY, weidth + maxWidth + 4, posY + height, Integer.MIN_VALUE);
   	    for (int i = 0; i < vals.size(); i++) {
            String string = vals.get(i).toString().toLowerCase();
            int textX = weidth + 2;
            int textY = posY + 2 + i * 12;

            if (isMouseOver(mouseX, mouseY, textX, textY - 2, maxWidth, 12)) {
                guiGraphics.drawString(this.font, string, textX, textY, 0xFFFF00FF);
            } else {
                guiGraphics.drawString(this.font, string, textX, textY, -1);
            }
        }
   }

   private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + height;
   }

   public Property<?> getProp(double x, double y, boolean number) {
   	    if (x < this.leftPos + 93 || x > this.leftPos + 159 || y < this.topPos + 24 || y > this.topPos + 118) return null;
   	    Collection<Property<?>> properties = this.playerState.getProperties();
   	    List<Property<?>> props = new ArrayList<>(properties);
   	    for (int i = 0; i < 5; i++) {
   	    	if (this.propOff + i < props.size()) {
   	    		if (y > this.topPos + 24 + i * 19 && y < this.topPos + 24 + i * 19 + 19) {
   	    		   if (number) this.listPropNumber = i;
   	    	       return props.get(this.propOff + i);
   	    		}
   	    	} else break;
   	    }
   	    if (number) this.listPropNumber = -1;
   	    return null;
   }

   public boolean mouseScrolled(double x, double y, double type) {
   	    if (x < this.leftPos + 93 || x > this.leftPos + 159 || y < this.topPos + 24 || y > this.topPos + 118) return false;
   	    Collection<Property<?>> properties = this.playerState.getProperties();
   	    if (!(this.getProp(x, y, false) instanceof IntegerProperty prop)) {
   	       if (type < 0 && this.propOff + 5 < properties.size()) {
              this.propOff++;
              this.listPropNumber--;
           } else if (type > 0 && this.propOff > 0) {
              this.propOff--;
              this.listPropNumber++;
           }
   	    } else {
   	       BlockState state = this.playerState;
   	       Collection<Integer> ints = prop.getPossibleValues();
   	       List<Integer> intes = new ArrayList<>(ints);
   	       int value = state.getValue(prop);
   	       if (type > 0 && value < intes.get(ints.size() - 1)) {
   	    	  state = state.setValue(prop, value + 1);
   	    	  sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
           } else if (type < 0 && value > intes.get(0)) {
              state = state.setValue(prop, value - 1);
              sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
           }
           ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(state, this.playerTag));
   	    }
   	    return true;
   }

   public boolean charTyped(char charo, int value) {
	    if (!init) {
	  	    init = true;
	  	    return false;
	    }
	    String s = this.tagsBox.getValue();
        if (this.tagsBox.charTyped(charo, value)) {
            if (!Objects.equals(s, this.tagsBox.getValue())) {
               this.updateNbt(this.tagsBox.getValue());
            }
            return true;
        }
        return false;
   }

   @Override
   public boolean keyPressed(int key, int b, int c) {
   	    if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		String s = tagsBox.getValue();
		if (this.tagsBox.keyPressed(key, b, c)) {
           if (!Objects.equals(s, this.tagsBox.getValue())) {
              this.updateNbt(this.tagsBox.getValue());
           }
           return true;
        }
		return super.keyPressed(key, b, c);
   }

   public boolean mouseClicked(double x, double y, int type) {
   	    if (type == 0) {
   	    	if (x > this.leftPos + 41 && x < this.leftPos + 4 + 80 && y > this.topPos - 19 && y < this.topPos - 19 + 22) {
   	    		MorphScreen sc = new MorphScreen(Component.literal("morph_screen"));
   	    		sc.init = true;
   	    		Minecraft.getInstance().setScreen(sc);
   	    		return true;
   	    	}
   	    	boolean flag = this.enumClick(x, y);
   	    	Property prop = this.getProp(x, y, true);
   	    	this.listProp = null;
   	    	if (prop != null && !flag) {
   	    		if (prop instanceof BooleanProperty bool) {
   	    			BlockState state = this.playerState;
   	    			state = state.setValue(bool, !this.playerState.getValue(bool));
                    ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(state, this.playerTag));
                    sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   	    	    } else if (prop instanceof EnumProperty enumprop) {
                    this.listProp = enumprop;
                    sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   	    	    }
   	    	}
   	    }
   	    return super.mouseClicked(x, y, type);
   }

   private int getLongWord(Collection<Enum<?>> values) {
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

   private boolean enumClick(double mouseX, double mouseY) {
   	    if (this.listProp != null && this.listPropNumber > -1 && this.listPropNumber < 5) {
   	        Collection<Enum<?>> values = listProp.getPossibleValues();
   	        List<Enum<?>> vals = new ArrayList<>(values);
            int maxWidth = this.getLongWord(values);
            int posY = this.topPos + 24 + this.listPropNumber * 19 + 6 + 8;
            int height = 12 * values.size();
            int weidth = this.leftPos + 93 + 40;
   	        for (int i = 0; i < vals.size(); i++) {
                String string = vals.get(i).toString().toLowerCase();
                int textX = weidth + 2;
                int textY = posY + 2 + i * 12;

                if (isMouseOver(mouseX, mouseY, textX, textY - 2, maxWidth, 12)) {
                	BlockState state = this.setEnum(this.listProp, string);
                    ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(state, this.playerTag));
                    sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
   	    }
   	    return false;
   }

   private <T extends Comparable<T>> BlockState setEnum(Property<T> prop, String s) {
      Optional<T> optional = prop.getValue(s);
      if (optional.isPresent()) {
      	 BlockState state = this.playerState;
         state = state.setValue(prop, optional.get());
         return state;
      } else {
         throw new IllegalArgumentException("Irregular value " + s + " for argument " + prop.getName());
      }
   }

   private void updateNbt(String s) {
   	    try {
   	    	CompoundTag tag = TagParser.parseTag(s);
   	    	BlockState blockState = this.playerState;
   	    	ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(blockState, tag));
   	    	this.tagException = "";
   	    } catch (CommandSyntaxException e) {
   	    	this.tagException = e.getMessage();
   	    }
   }


   @Override
   public void init() {
		super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        tagsBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 145, 158, 17, null);
		tagsBox.setMaxLength(32767);
		tagsBox.setBordered(false);
		tagsBox.setTextColor(-1);
		tagsBox.setTextColorUneditable(-1);
		this.addWidget(tagsBox);
		this.playerState = ((PlayerAccessor)this.entity).getBlockState();
		BlockState blockState = this.playerState;
		if (blockState.getBlock() instanceof EntityBlock) {
			this.setInitialFocus(tagsBox);
			CompoundTag tag = ((PlayerAccessor)this.entity).getTag();
			tagsBox.setValue(tag.toString());
			this.playerTag = tag;
		} else {
			tagsBox.setFocused(false);
			tagsBox.setEditable(false);
		}
   }

   public void tick() {
   	    super.tick();
   	    tagsBox.tick();
   }

   @Override
   public void resize(Minecraft minecraft, int width, int height) {
		String tagsBoxV = tagsBox.getValue();
		super.resize(minecraft, width, height);
		tagsBox.setValue(tagsBoxV);
   }

   public void renderBlockAsIcon(GuiGraphics guiGraphics, float ticks) {
   	    PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
        poseStack.pushPose();
        poseStack.translate(this.leftPos + 71, this.topPos + 63.8, 20); 
        poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
        poseStack.scale(36.0F, 36.0F, 36.0F); 
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(225.0F)); 
        BlockPos pos = AIR;
        BlockState blockstate = this.playerState;
        RandomSource random = RandomSource.create(blockstate.getSeed(pos));
        var model = this.dispatcher.getBlockModel(blockstate);
        var renderType = ItemBlockRenderTypes.getMovingBlockRenderType(blockstate);
        this.dispatcher.getModelRenderer().tesselateBlock(world, model, blockstate, pos, poseStack, bufferSource.getBuffer(renderType), false, RandomSource.create(), blockstate.getSeed(pos), OverlayTexture.NO_OVERLAY);
        this.renderBlockEntity(blockstate, ticks, poseStack, bufferSource);
        poseStack.popPose();
   }

   private void renderBlockEntity(BlockState blockstate, float partialticks, PoseStack posestack, MultiBufferSource buffer) {
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(AIR, blockstate);
            if (blockEntity != null) {
              try {
      	        blockEntity.setLevel(world);
      	        blockEntity.load(playerTag);
                BlockEntityRenderer renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
                if (renderer != null) {
           	        posestack.pushPose();
                    renderer.render(blockEntity, partialticks, posestack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
                    posestack.popPose();
                }
              } catch (Exception e) {
           	    this.tagException = e.getMessage();	
              }
           }  
        }
   }
   
}
