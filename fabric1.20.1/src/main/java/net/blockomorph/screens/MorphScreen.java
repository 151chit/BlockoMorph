package net.blockomorph.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.BlockomorphMod;
import net.blockomorph.network.ServerBoundBlockMorphPacket;
import net.blockomorph.screens.BlockMorphConfigScreen;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

@Environment(EnvType.CLIENT)
public class MorphScreen extends Screen {
	private final HashMap<String, List<Block>> content;
	private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
	private static final ResourceLocation CREATIVE_TABS_LOCATION = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
	private static final ResourceLocation PACKET_ID = new ResourceLocation(BlockomorphMod.MOD_ID, "server_bound_block_morph_packet");
	private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
	private static final CreativeModeTab search = getTab(CreativeModeTabs.SEARCH);
	private static final CreativeModeTab op_tab = getTab(CreativeModeTabs.OP_BLOCKS);
	public static final List<CreativeModeTab> tabs = getUnStandartTabs();
	private List<Block> reg;
	private static List<Block> list = new ArrayList<>();
	private final static BlockPos AIR = new BlockPos(0, 512, 0); 
	private final Level world;
	private final Player entity;
	public boolean init;
	protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int leftPos;
    protected int topPos;
	private int scrollOff;
	private boolean scrollWork;
	private static int page = 0;
	private static int pageCount = 1;
	EditBox searchBox;

	private static final ResourceLocation texture = new ResourceLocation("blockomorph:textures/screens/morph_gui.png");

	public MorphScreen(Component text) {
		super(text);
		Minecraft mc = Minecraft.getInstance();
		this.world = mc.level;
		this.entity = mc.player;
		LocalPlayer pl = mc.player;
		this.reg = getRegistredBlocks();
		CreativeModeTabs.tryRebuildTabContents(pl.level().enabledFeatures(), mc.options.operatorItemsTab().get() && pl.canUseGameMasterBlocks(), pl.level().registryAccess());
        this.content = this.sortBlocksByTabs(this.reg, CreativeModeTabs.tabs());
        pageCount = (int) Math.ceil((double) tabs.size() / 10);
	}

	public List<Block> getRegistredBlocks() {
		List<Block> blocks = new ArrayList<>();
		for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
             blocks.add(entry.getValue());
        }
        return blocks;
	}

	private static List<CreativeModeTab> getUnStandartTabs() {
		List<CreativeModeTab> t = new ArrayList<>();
		List<CreativeModeTab> f = new ArrayList<>();
		t.add(getTab(CreativeModeTabs.HOTBAR));
        t.add(search);
        t.add(op_tab);
        t.add(getTab(CreativeModeTabs.INVENTORY));
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (!t.contains(tab))
                f.add(tab);
        }
        return f;
	}

	public static HashMap<String, List<Block>> sortBlocksByTabs(List<Block> blocks, List<CreativeModeTab> tabs) {
        HashMap<String, List<Block>> sortedBlocks = new HashMap<>();

        sortedBlocks.put("unsortable", new ArrayList<>());

        for (Block block : blocks) {
            ItemStack itemStack = block.asItem().getDefaultInstance();
            if (itemStack == null || itemStack.isEmpty()) {
                sortedBlocks.get("unsortable").add(block);
            } else {
            	boolean flag = false;
                for (CreativeModeTab tab : tabs) {
                    if (tab != search && tab.contains(itemStack)) {
                        sortedBlocks.computeIfAbsent(getName(tab).toString(), k -> new ArrayList<>()).add(block);
                        flag = true;
                    }
                }
                if (!flag) sortedBlocks.get("unsortable").add(block);
            }
        }

        return sortedBlocks;
    }

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		if (pageCount > 1) {
		    Component page = Component.literal(String.format("%d / %d", MorphScreen.page + 1, pageCount));
		    guiGraphics.drawString(this.font, page.getVisualOrderText(), this.leftPos + (this.imageWidth / 2) - (this.font.width(page) / 2), this.topPos - 34, -1);
		}
		if (selectedTab.showTitle())
		    guiGraphics.drawString(this.font, selectedTab.getDisplayName(), this.leftPos + 8, this.topPos + 6, 0x404040, false);
		this.renderBlockAsIcon(guiGraphics, partialTicks);
		if (this.hasSearchBar())
		    searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);
		Block bl = this.findBlockClick(mouseX, mouseY);
		if (bl != null) {
		    guiGraphics.renderTooltip(this.font, bl.getName(), mouseX, mouseY);
		} else {
			CreativeModeTab tab = this.getTabAtPosition(mouseX, mouseY);
			if (tab != null) guiGraphics.renderTooltip(this.font, tab.getDisplayName(), mouseX, mouseY);
		}
	}

	public void refreshList() {
		this.list.clear();
		if (selectedTab == search) {
		    this.list.addAll(this.reg);
		} else if (selectedTab == op_tab) {
			if (content.containsKey("minecraft:op_blocks")) this.list.addAll(content.get("minecraft:op_blocks"));
			this.list.addAll(content.get("unsortable"));
		} else {
			String name = this.getName(selectedTab).toString();
			if (content.containsKey(name)) this.list.addAll(content.get(name));
		}
	}

	@Nullable
    public static CreativeModeTab getTab(ResourceLocation name) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(name);
    }

    @Nullable
    public static CreativeModeTab getTab(ResourceKey name) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(name);
    }

    @Nullable
    public static ResourceLocation getName(CreativeModeTab tab) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
    }

	public void searchBlock(String searchName) {
		if (searchName.equals("")) {
			this.refreshList();
		}
		this.list.clear();
		String name = this.getName(selectedTab).toString();
		if (content.containsKey(name) || selectedTab == search) {
		   List<Block> searcheable;
		   if (selectedTab == search) {
		   	   searcheable = this.reg;
		   } else {
		       searcheable = content.get(name);
		   }
		
		   List<Block> foundBlocks = searcheable.stream()
               .filter(block -> block.getName().getString().toLowerCase().contains(searchName.toLowerCase()))
               .collect(Collectors.toList());
           this.list.addAll(foundBlocks);
		}
        this.scrollOff = 0;
	}

	public void renderBlockAsIcon(GuiGraphics guiGraphics, float ticks) {
        PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
        int xO = 0;
        int yO = 0;
        
        for (int i = this.scrollOff; i < 16 + this.scrollOff; i++) {
           poseStack.pushPose();

           poseStack.translate(this.leftPos + 42 + xO*36, this.topPos + 41.8 + yO*36, 20); 
           poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
           poseStack.scale(20.0F, 20.0F, 20.0F); 
           poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
           poseStack.mulPose(Axis.YP.rotationDegrees(225.0F)); 

      	   BlockPos pos = AIR;
      	   if (this.scrollOff + i < list.size()) {
      	      BlockState blockState = list.get(this.scrollOff + i).defaultBlockState();
              RandomSource random = RandomSource.create(blockState.getSeed(pos));
              if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                 var model = this.dispatcher.getBlockModel(blockState);
                 var renderType = ItemBlockRenderTypes.getMovingBlockRenderType(blockState);
                 this.dispatcher.getModelRenderer().tesselateBlock(world, model, blockState, pos, poseStack, bufferSource.getBuffer(renderType), false, RandomSource.create(), blockState.getSeed(pos), OverlayTexture.NO_OVERLAY);
              } else if (blockState.getBlock().asItem() != null && !(blockState.getBlock() instanceof EntityBlock)) {
                    //in development
              }
              this.renderBlockEntity(blockState, ticks, poseStack, bufferSource); 
              poseStack.popPose();
              if (((PlayerAccessor)entity).getBlockState().getBlock() == blockState.getBlock()) guiGraphics.blit(new ResourceLocation("blockomorph:textures/screens/selected.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
           } else {
           	  poseStack.popPose();
      		  break;
           }

           xO++;
           if (xO > 3) {
           	  xO = 0;
        	  yO++;
           }
        }
    }

    private Block findBlockClick(double x, double y) {
        int leftPos = this.leftPos + 11;
        int topPos = this.topPos + 16;

        if (x < leftPos || x >= leftPos + 4 * 35.5 || y < topPos || y >= topPos + 4 * 35.5) {
            return null; 
        }

        int col = (int) ((x - leftPos) / 35.5);
        int row = (int) ((y - topPos) / 35.5);

        int index = row * 4 + col;

        int block = index + this.scrollOff * 2;
        if (block >= list.size()) return null;
        return list.get(block);
    }

    private CreativeModeTab getTabAtPosition(double x, double y) {
    	int l = this.leftPos + this.imageWidth - 38;
    	int i1 = this.getTabY(-1);
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32) return search;
    	l = this.leftPos + this.imageWidth - 70;
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32) return op_tab;
    	for (int i = 0; i < 10; i++) {

        l = this.leftPos;
        i1 = this.getTabY(i);

        if (i < 5) {
            l -= 28;
        } else {
            l += this.imageWidth - 4;
        }
        if (x > l && x < l + 32 && y > i1 && y < i1 + 28 && 10 * page + i < tabs.size()) {
            return tabs.get(10 * page + i); 
        }
        
        }

        return null; 
    }

    private void renderBlockEntity(BlockState blockstate, float partialticks, PoseStack posestack, MultiBufferSource buffer) {
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(AIR, blockstate);
            if (blockEntity != null) {
      	        blockEntity.setLevel(world);
                BlockEntityRenderer renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
                if (renderer != null) {
           	        posestack.pushPose();
           	        try {
                        renderer.render(blockEntity, partialticks, posestack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
           	        } catch (Exception e) {
           	        	
                    }
                    posestack.popPose();
                }
           }  
        }
    }

    public void selectTab(CreativeModeTab tab) {
    	this.selectedTab = tab;
    	searchBox.setFocused(this.hasSearchBar());
    	searchBox.setValue("");
    	this.refreshList();
    	this.scrollOff = 0;
    }

    public boolean hasSearchBar() {
    	if (selectedTab == search) return true;
    	return false;
    }

    protected void renderTabButton(GuiGraphics gui, CreativeModeTab tab, int i, boolean isLeft) {
        boolean flag = tab == selectedTab;
        int j = 32;
        int k = 64;
        int l = this.leftPos;
        int i1 = this.getTabY(i);
        int weight = 32;
        int height = 28;
        if (flag) k = 92;
        if (!isLeft) j = 128;

        if (isLeft) {
            l -= 28; 
        } else {
            l += this.imageWidth - 4; 
        }
        if (i < 0) {
        	j = 112;
        	if (flag) {
        		k = 32;
        	} else {
        		k = 0;
        	}
        	l += this.imageWidth - 10;
        	if (i == -2) l -= 32;
        	height = 32;
        	weight = 28;
        }
        
        gui.blit(AdvancementsScreen.TABS_LOCATION, l, i1, j, k, weight, height);

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 100.0F);

        ItemStack itemstack = tab.getIconItem();
        gui.renderItem(itemstack, l + 7, i1 + 5);
        gui.renderItemDecorations(this.font, itemstack, l + 7, i1 + 4);
        gui.pose().popPose();
    }

    public int getTabY(int i) {
    	if (i < 0) return this.topPos + this.imageHeight - 4;
    	if (i > 4) i -= 5;
    	int pos = this.topPos + 3;
    	return pos += i * 32;
    }


	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		if (this.hasSearchBar())
		    guiGraphics.blit(new ResourceLocation("blockomorph:textures/screens/searchbar.png"), this.leftPos + 90, this.topPos - 19, 0, 0, 80, 23, 80, 23);
		int j = 0;
		for (int i = page * 10; i < page * 10 + 10; i++) {
			if (i < tabs.size()) {
		       this.renderTabButton(guiGraphics, tabs.get(i), j, j < 5);
		       j++;
			} else break;
		}
		this.renderTabButton(guiGraphics, op_tab, -2, true);
		this.renderTabButton(guiGraphics, search, -1, true);
		guiGraphics.blit(new ResourceLocation("blockomorph:textures/screens/exit_tabs.png"), this.leftPos + 4, this.topPos - 19, 0, 0, 80, 22, 80, 46);
		int yPos = this.topPos + 16;
		int totalScrollableElements = this.list.size() - 16;

        double scrollPercentage = (double)this.scrollOff / totalScrollableElements;
        int sharp = (int)Math.round(scrollPercentage * (253));
        sharp = Mth.clamp(sharp, 0, 127);
		guiGraphics.blit(CREATIVE_TABS_LOCATION, this.leftPos + 158, yPos + sharp, 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
		RenderSystem.disableBlend();
	}

	public boolean mouseClicked(double x, double y, int type) {
		if (type == 0) {
			if (x > this.leftPos + 4 && x < this.leftPos + 4 + 41 && y > this.topPos - 19 && y < this.topPos - 19 + 22) {
				BlockMorphConfigScreen sc = new BlockMorphConfigScreen(Component.literal("morph_screen"));
				sc.init = true;
   	    		Minecraft.getInstance().setScreen(sc);
   	    		return true;
   	    	}
			Block bl = this.findBlockClick(x, y);
			if (bl != null) {
				BlockState state = bl.defaultBlockState();
				ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(state, new CompoundTag()));
				return true;
			} else {
				CreativeModeTab tab = this.getTabAtPosition(x, y);
				if (tab != null) {
				    this.selectTab(tab);
				    return true;
				} else {
					if (x > this.leftPos + 157 && x < this.leftPos + 157 + 13 && y > this.topPos + 15 && y < this.topPos + 158) {
						this.scrollWork = this.canScroll();
					}
				}
			}
		}
		return super.mouseClicked(x, y, type);
	}

	private boolean canScroll() {
		if (this.list.size() > 16) return true;
		return false;
	}

	public boolean mouseDragged(double x, double y, int type, double prevX, double prevY) {
		if (this.scrollWork) {
        int sharp = (int)Math.floor(y - 7d) - (this.topPos + 16);
        int totalScrollableElements = this.list.size() - 16;

        double scrollPercentage = (double) sharp / 127;

        int scrollOff = (int)Math.floor(scrollPercentage * totalScrollableElements);
        int lock = 7;
        if (list.size() % 4 == 0) lock++;

        scrollOff = Mth.clamp(scrollOff / 2, 0, (list.size() / 2) - lock);
        if (scrollOff % 2 != 0.0) scrollOff++;
        this.scrollOff = scrollOff;
            
		}
		return super.mouseDragged(x, y, type, prevX, prevY);
	}

	public boolean mouseReleased(double x, double y, int type) {
		if (type == 0) {
			this.scrollWork = false;
			Block bl = this.findBlockClick(x, y);
			if (bl != null) {
				BlockState state = bl.defaultBlockState();
				ClientPlayNetworking.send(PACKET_ID, ServerBoundBlockMorphPacket.create(state, new CompoundTag()));
				return true;
			} else {
				CreativeModeTab tab = this.getTabAtPosition(x, y);
				if (tab != null) {
				    this.selectTab(tab);
				    return true;
				}
			}
		}
		return super.mouseReleased(x, y, type);
	}

	public boolean mouseScrolled(double p_98527_, double p_98528_, double p_98529_) {
		if (p_98529_ < 0 && this.scrollOff * 2 + 16 < list.size()) {
            this.scrollOff = this.scrollOff + 2;
        } else if (p_98529_ > 0 && this.scrollOff > 0) {
            this.scrollOff = this.scrollOff - 2;
        }
        return true; 
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
	    String s = this.searchBox.getValue();
        if (this.searchBox.keyPressed(key, b, c)) {
           if (!Objects.equals(s, this.searchBox.getValue())) {
              this.searchBlock(this.searchBox.getValue());
           }
           return true;
        }
		return super.keyPressed(key, b, c);
	}

	public boolean isPauseScreen() {
      return false;
    }

	public boolean charTyped(char p_98521_, int p_98522_) {
	  if (!init) {
	  	 init = true;
	  	 return false;
	  }
      if (!this.hasSearchBar()) {
         return false;
      } else {
         String s = this.searchBox.getValue();
         if (this.searchBox.charTyped(p_98521_, p_98522_)) {
            if (!Objects.equals(s, this.searchBox.getValue())) {
               this.searchBlock(this.searchBox.getValue());
            }
            return true;
         } else {
            return false;
         }
      }
    }

    public void setPage(boolean up) {
    	if (up) {
    	    if (page + 1 < pageCount) {
    		    page++;
    	    }
    	} else {
    	    if (page > 0) {
    		    page--;
    	    }
    	}
    }


	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String searchBoxValue = searchBox.getValue();
		super.resize(minecraft, width, height);
		searchBox.setValue(searchBoxValue);
	}

	@Override
	public void tick() {
		searchBox.tick();
	}

	@Override
	public void init() {
		super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
		this.refreshList();
		searchBox = new EditBox(this.font, this.leftPos + 99, this.topPos + -10, 70, 12, null);
		searchBox.setMaxLength(32767);
		searchBox.setBordered(false);
		searchBox.setTextColor(16777215);
		this.addWidget(searchBox);
		if (this.hasSearchBar()) this.setInitialFocus(this.searchBox);
		if (pageCount > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.setPage(false)).pos(leftPos - 22,  topPos - 22).size(20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.setPage(true)).pos(leftPos + imageWidth - 0, topPos - 22).size(20, 20).build());
        }
	}
}
