package net.blockomorph.screens;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.Blockomorph;
import net.blockomorph.utils.*;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.config.*;
import net.blockomorph.network.*;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.multiplayer.SessionSearchTrees;

@OnlyIn(Dist.CLIENT)
public class MorphScreen extends Screen {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
	private final HashMap<String, List<Block>> content;
	private final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
	private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
	private static final CreativeModeTab search = CreativeModeTabRegistry.getTab(ResourceLocation.withDefaultNamespace("search"));
	private static final CreativeModeTab op_tab = CreativeModeTabRegistry.getTab(ResourceLocation.withDefaultNamespace("op_blocks"));
	private static final CreativeModeTab loved_blocks = CreativeModeTabRegistry.getTab(ResourceLocation.withDefaultNamespace("hotbar"));
	private static final CreativeModeTab allowed = CreativeModeTab.builder().title(Component.translatable("gui.blockomorph.allowedBlocks")).withSearchBar().icon(() -> {return new ItemStack(Items.NETHER_STAR);}).build();
    private static final WidgetSprites DEMORPH_BUT = new WidgetSprites(
   	 ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/demorph_def.png"),
   	 ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/demorph_dis.png"),
   	 ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/demorph_hov.png")
    );
	private static final WidgetSprites FLAME_BUT = new WidgetSprites(
			ResourceLocation.fromNamespaceAndPath(Blockomorph.MODID, "textures/screens/flame_def.png"),
			ResourceLocation.fromNamespaceAndPath(Blockomorph.MODID, "textures/screens/flame_dis.png"),
			ResourceLocation.fromNamespaceAndPath(Blockomorph.MODID, "textures/screens/flame_hov.png")
	);
	public static final List<CreativeModeTab> tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs();
	private List<Block> reg;
	private static List<Block> list = new ArrayList<>();
	private final static BlockPos AIR = new BlockPos(0, 512, 0); 
	private final List<SavedBlock> savedBlockContent = new ArrayList<>();
	private final List<SavedBlock> savedBlocks = new ArrayList<>();
	private final Level world;
	private final Player entity;
	public boolean init;
	protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int leftPos;
    protected int topPos;
    protected MultiBufferSource tempBufer;
	private int scrollOff;
	private boolean scrollWork;
	private static int page = 0;
	private static int pageCount = 1;
	private final Config.Mode mode;
	EditBox searchBox;
	ImageButton unmask;
	ImageButton fuse;

	private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/morph_gui.png");

	public MorphScreen(Config.Mode mode, boolean init) {
		super(Component.literal("morph_screen"));
		this.mode = mode;
		this.init = init;
		MorphUtils.bmanager.load();
		if (this.isConfig() && selectedTab == allowed) this.selectedTab = CreativeModeTabs.getDefaultTab();
		Minecraft mc = Minecraft.getInstance();
		this.world = mc.level;
		this.entity = mc.player;
		this.reg = this.getRegistredBlocks();
		this.loadCreativeBlocks(mc);
        this.content = this.sortBlocksByTabs(this.reg, CreativeModeTabs.tabs());
        pageCount = (int) Math.ceil((double) tabs.size() / 10);
        for (SavedBlock s : MorphUtils.bmanager.get().values()) {
        	this.savedBlockContent.add(s);
        }
	}

	public List<Block> getRegistredBlocks() {
		List<Block> blocks = new ArrayList<>();
		for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
             blocks.add(entry.getValue());
        }
        return blocks;
	}

	private void loadCreativeBlocks(Minecraft mc) {
		LocalPlayer pl = mc.player;
		if (CreativeModeTabs.tryRebuildTabContents(
			pl.connection.enabledFeatures(), 
			mc.options.operatorItemsTab().get() && pl.canUseGameMasterBlocks(), 
			pl.level().registryAccess()
		)) {
			SessionSearchTrees sessionSearchTrees = pl.connection.searchTrees();
			if (sessionSearchTrees != null) {
				List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
                sessionSearchTrees.updateCreativeTooltips(pl.level().registryAccess(), list);
                sessionSearchTrees.updateCreativeTags(list);
			}
		}
	}

	public boolean isConfig() {
		return this.mode != Config.Mode.NONE;
	}

	public HashMap<String, List<Block>> sortBlocksByTabs(List<Block> blocks, List<CreativeModeTab> tabs) {
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
                        sortedBlocks.computeIfAbsent(CreativeModeTabRegistry.getName(tab).toString(), k -> new ArrayList<>()).add(block);
                        flag = true;
                    }
                }
                if (!flag) sortedBlocks.get("unsortable").add(block);
            }
        }

        sortedBlocks.put("allowed", (this.reg.stream()
            .filter(block -> MorphUtils.isBannedBlock(block.defaultBlockState(), entity) == null)
            .collect(Collectors.toList())));

        return sortedBlocks;
    }

    public void updateAllowed() {
    	this.content.put("allowed", (this.reg.stream()
            .filter(block -> MorphUtils.isBannedBlock(block.defaultBlockState(), entity) == null)
            .collect(Collectors.toList())));
        this.unmask.visible = this.needUnmorphBut();
    }

    private void extractBuffer(MultiBufferSource b) {
   	    this.tempBufer = b;
    }

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
		if (pageCount > 1) {
		    Component page = Component.literal(String.format("%d / %d", MorphScreen.page + 1, pageCount));
		    guiGraphics.drawString(this.font, page.getVisualOrderText(), this.leftPos + (this.imageWidth / 2) - (this.font.width(page) / 2), this.topPos - 34, -1);
		}
		if (selectedTab.showTitle())
		    guiGraphics.drawString(this.font, selectedTab.getDisplayName(), this.leftPos + 8, this.topPos + 6, selectedTab.getLabelColor(), false);
		guiGraphics.drawSpecial(this::extractBuffer);
		this.renderBlockAsIcon(guiGraphics, partialTicks);
		int i = this.findBlockIndex(mouseX, mouseY);
		if (i != -1) {
			Block bl = this.findBlockClick(mouseX, mouseY);
			if (bl != null) {
		        guiGraphics.renderTooltip(this.font, bl.getName(), mouseX, mouseY);
			} else {
				int ind = i + this.scrollOff * 2;
				if (ind >= this.savedBlocks.size()) return;
				guiGraphics.renderTooltip(this.font, Component.literal(this.savedBlocks.get(ind).getName()), mouseX, mouseY);
			}
		} else {
			CreativeModeTab tab = this.getTabAtPosition(mouseX, mouseY);
			if (tab != null) guiGraphics.renderTooltip(this.font, tab.getDisplayName(), mouseX, mouseY);
		}
		if (this.unmask != null) this.unmask.active = ((PlayerAccessor)this.entity).isFullActive();
		if (this.fuse != null) {
			this.fuse.active = this.activeFlameBut();
			this.fuse.visible = this.needFlameBut();
		}
	}

	public void refreshList() {
		this.list.clear();
		this.savedBlocks.clear();
		if (selectedTab == loved_blocks) {
			this.savedBlocks.addAll(this.savedBlockContent);
			for (SavedBlock b : savedBlockContent) {
				this.list.add(null);
			}
			return;
		}
		if (selectedTab == search) {
		    this.list.addAll(this.reg);
		} else if (selectedTab == allowed) { 
			this.list.addAll(content.get("allowed"));
		} else if (selectedTab == op_tab) {
			if (content.containsKey("minecraft:op_blocks")) this.list.addAll(content.get("minecraft:op_blocks"));
			this.list.addAll(content.get("unsortable"));
		} else {
			String name = CreativeModeTabRegistry.getName(selectedTab).toString();
			if (content.containsKey(name)) this.list.addAll(content.get(name));
		}
	}

	public void searchBlock(String searchName) {
		if (searchName.equals("")) {
			this.refreshList();
		}
		this.list.clear();
		this.savedBlocks.clear();
		if (selectedTab == loved_blocks) {
			for (SavedBlock entry : this.savedBlockContent) {
				if (entry.getName().toLowerCase().contains(searchName.toLowerCase())) {
					this.savedBlocks.add(entry);
					this.list.add(null);
				}
			}
			return;
		}
		String name = "";
		if (selectedTab != allowed)
		   name = CreativeModeTabRegistry.getName(selectedTab).toString();
		if (content.containsKey(name) || selectedTab == search || selectedTab == allowed) {
		   List<Block> searcheable;
		   if (selectedTab == search) {
		   	   searcheable = this.reg;
		   } else if (selectedTab == allowed) {
		       searcheable = content.get("allowed");
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
        MultiBufferSource bufferSource = this.tempBufer;
        int xO = 0;
        int yO = 0;
        
        for (int i = this.scrollOff; i < 16 + this.scrollOff; i++) {
      	   if (this.scrollOff + i < ((selectedTab == loved_blocks) ? this.savedBlocks.size() : list.size())) {
      	   	  BlockState blockState;
      	   	  CompoundTag tag = null;
      	   	  if (selectedTab == loved_blocks) {
      	   	  	SavedBlock b = savedBlocks.get(this.scrollOff + i);
      	   	  	blockState = b.getState();
      	   	  	tag = b.getTag();
      	   	  } else {
      	   	  	blockState = list.get(this.scrollOff + i).defaultBlockState();
      	   	  }

              poseStack.pushPose();
              this.renderBlock(poseStack, bufferSource, xO, yO, blockState, ticks, tag);
              poseStack.popPose();

              poseStack.pushPose();
              poseStack.translate(0, 0, 200); 
              this.renderFrame(guiGraphics, blockState, xO, yO, tag);
              poseStack.translate(0, 0, -200); 
              poseStack.popPose();
              
           } else {
      		  break;
           }

           xO++;
           if (xO > 3) {
           	  xO = 0;
        	  yO++;
           }
        }
    }

    private void renderBlock(PoseStack poseStack, MultiBufferSource bufferSource, int xO, int yO, BlockState blockState, float ticks, @Nullable CompoundTag tag) {
    	poseStack.translate(this.leftPos + 42 + xO*36, this.topPos + 41.8 + yO*36, 100); 
        poseStack.mulPose((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
        poseStack.scale(20.0F, 20.0F, 20.0F); 
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(225.0F)); 

      	BlockPos pos = AIR;
        RandomSource random = RandomSource.create(blockState.getSeed(pos));
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
           var model = this.dispatcher.getBlockModel(blockState);
			var modeldata = model.getModelData(world, pos, blockState, ModelData.EMPTY);
			for (var renderType : model.getRenderTypes(blockState, random, modeldata)) {
				VertexConsumer vertex = bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
				this.dispatcher.getModelRenderer().tesselateBlock(world, model, blockState, pos, poseStack, vertex, false, RandomSource.create(), blockState.getSeed(pos), OverlayTexture.NO_OVERLAY, modeldata, renderType);
			}
        } else if (blockState.getBlock().asItem() != null && !(blockState.getBlock() instanceof EntityBlock)) {
              //in development
        }
        this.renderBlockEntity(blockState, ticks, poseStack, bufferSource, tag); 
    }

    private void renderFrame(GuiGraphics guiGraphics, BlockState blockState, int xO, int yO, @Nullable CompoundTag tag) {
    	String name = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        if (!this.isConfig()) {
        	if (MorphUtils.isBannedBlock(blockState, entity) != null) {
        		guiGraphics.blit(RenderType::guiTextured, ResourceLocation.tryParse("blockomorph:textures/screens/sel_lock.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
        		return;
        	}
        	BlockState plSt = ((PlayerAccessor)entity).getBlockState(InPlayerBlockPos.ZERO);
            if (selectedTab == loved_blocks) {
            	if (plSt.equals(blockState) && tag.equals(((PlayerAccessor)entity).getTag(InPlayerBlockPos.ZERO)))
            	guiGraphics.blit(RenderType::guiTextured, ResourceLocation.tryParse("blockomorph:textures/screens/selected.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
            } else if (plSt.getBlock() == blockState.getBlock()) {
            	guiGraphics.blit(RenderType::guiTextured, ResourceLocation.tryParse("blockomorph:textures/screens/selected.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
            }
        } else if (this.mode == Config.Mode.WHITELIST) {
            if (((List<String>)Config.getInstance().getValue("allowedBlocks")).contains(name)) guiGraphics.blit(RenderType::guiTextured, ResourceLocation.tryParse("blockomorph:textures/screens/sel_good.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
        } else if (this.mode == Config.Mode.BLACKLIST) {
            if (((List<String>)Config.getInstance().getValue("bannedBlocks")).contains(name)) guiGraphics.blit(RenderType::guiTextured, ResourceLocation.tryParse("blockomorph:textures/screens/sel_bad.png"), this.leftPos + 10 + xO*36, this.topPos + 15 + yO*36, 0, 0, 36, 36, 36, 36);
        }
    }

    private Block findBlockClick(double x, double y) {
    	int i = this.findBlockIndex(x, y);
    	if (i == -1 || selectedTab == loved_blocks) return null;
        int block = i + this.scrollOff * 2;
        if (block >= list.size()) return null;
        return list.get(block);
    }

    private int findBlockIndex(double x, double y) {
    	int leftPos = this.leftPos + 11;
        int topPos = this.topPos + 16;

        if (x < leftPos || x >= leftPos + 4 * 35.5 || y < topPos || y >= topPos + 4 * 35.5) {
            return -1; 
        }

        int col = (int) ((x - leftPos) / 35.5);
        int row = (int) ((y - topPos) / 35.5);

        int index = row * 4 + col;
        return index;
    }

    private boolean needAllowedTab() {
    	return !this.isConfig() && ((Config.Mode)Config.getInstance().getValue("listMode") != Config.Mode.NONE || (boolean)Config.getInstance().getValue("solidBlocksOnly"));
    }

    private CreativeModeTab getTabAtPosition(double x, double y) {
    	int l = this.leftPos + this.imageWidth - 38;
    	int i1 = this.getTabY(-1);
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32) return search;
    	l = this.leftPos + this.imageWidth - 70;
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32) return op_tab;
    	l = this.leftPos + this.imageWidth - 134;
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32 && this.needAllowedTab()) return allowed;
    	l = this.leftPos + this.imageWidth - 102;
    	if (x > l && x < l + 28 && y > i1 && y < i1 + 32) return loved_blocks;
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

    private void renderBlockEntity(BlockState blockstate, float partialticks, PoseStack posestack, MultiBufferSource buffer, @Nullable CompoundTag tag) {
   	    if (blockstate.getBlock() instanceof EntityBlock ent) {
            BlockEntity blockEntity = ent.newBlockEntity(AIR, blockstate);
            if (blockEntity != null) {
      	        blockEntity.setLevel(world);
      	        if (tag != null) blockEntity.loadWithComponents(tag, entity.level().registryAccess());
                BlockEntityRenderer renderer = blockEntityRenderDispatcher.getRenderer(blockEntity);
                if (renderer != null) {
           	        posestack.pushPose();
           	        try {
						ClientLevelAccessor acc = ClientLevelAccessor.of(world);
						acc.setBlockEntityRenderingMode(true);
                        renderer.render(blockEntity, partialticks, posestack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
						acc.setBlockEntityRenderingMode(false);
           	        } catch (Exception e) {
           	        	
                    }
                    posestack.popPose();
                }
           }  
        }
    }

    public void selectTab(CreativeModeTab tab) {
    	this.selectedTab = tab;
    	searchBox.setFocused(true);
    	searchBox.active = this.hasSearchBar();
    	searchBox.setValue("");
    	this.refreshList();
    	this.scrollOff = 0;
    }

    protected void renderTabButton(GuiGraphics gui, CreativeModeTab tab, int i, boolean isLeft) {
        boolean flag = tab == selectedTab;
        String tabType;
        int l = this.leftPos;
        int i1 = this.getTabY(i);
        int weight = 32;
        int height = 28;

        if (isLeft) {
            l -= 28; 
            tabType = "left";
        } else {
            l += this.imageWidth - 4; 
            tabType = "right";
        }
        if (i < 0) {
        	tabType = "below";
        	l += this.imageWidth - 10;
        	if (i == -2) l -= 32;
        	if (i == -3) l -= 96;
        	if (i == -4) l -= 64;
        	weight = 28;
        	height = 32;
        }
        tabType = tabType + "_middle";
        if (flag) tabType = tabType + "_selected";
        
        gui.blit(RenderType::guiTextured, ResourceLocation.withDefaultNamespace("textures/gui/sprites/advancements/tab_" + tabType + ".png"), l, i1, 0, 0, weight, height, weight, height);

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

    public boolean hasSearchBar() {
    	return selectedTab.hasSearchBar() || selectedTab == loved_blocks;
    }

	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(RenderType::guiTextured, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		if (this.hasSearchBar())
		    guiGraphics.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/searchbar.png"), this.leftPos + 90, this.topPos - 19, 0, 0, 80, 23, 80, 23);
		int j = 0;
		for (int i = page * 10; i < page * 10 + 10; i++) {
			if (i < tabs.size()) {
		       this.renderTabButton(guiGraphics, tabs.get(i), j, j < 5);
		       j++;
			} else break;
		}
		this.renderTabButton(guiGraphics, loved_blocks, -4, true);
		if (this.needAllowedTab()) this.renderTabButton(guiGraphics, allowed, -3, true);
		this.renderTabButton(guiGraphics, op_tab, -2, true);
		this.renderTabButton(guiGraphics, search, -1, true);
		guiGraphics.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("blockomorph", "textures/screens/exit_tabs.png"), this.leftPos + 4, this.topPos - 19, 0, 0, 80, 22, 80, 46);
		int yPos = this.topPos + 16;
		int totalScrollableElements = this.list.size() - 16;

        double scrollPercentage = (double)this.scrollOff / totalScrollableElements;
        int sharp = (int)Math.round(scrollPercentage * (253));
        sharp = Mth.clamp(sharp, 0, 127);
		guiGraphics.blitSprite(RenderType::guiTextured, this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE, this.leftPos + 158, yPos + sharp, 12, 15);
		RenderSystem.disableBlend();
	}

	public boolean mouseClicked(double x, double y, int type) {
		if (type == 0) {
			if (x > this.leftPos + 4 && x < this.leftPos + 4 + 41 && y > this.topPos - 19 && y < this.topPos - 19 + 22) {
				this.minecraft.setScreen(new BlockMorphConfigScreen(true));
   	    		return true;
   	    	}
			int i = this.findBlockIndex(x, y);
			if (i != -1) {
				String ac;
				BlockState st;
				CompoundTag tg = new CompoundTag();
				Block bl = this.findBlockClick(x, y);
				if (bl != null) {
					st = bl.defaultBlockState();
				} else {
					int ind = i + this.scrollOff * 2;
					if (ind >= this.savedBlocks.size()) return true;
					SavedBlock blo = this.savedBlocks.get(ind);
					st = blo.getState();
					tg = blo.getTag();
				}
				String name = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
				if (!this.isConfig()) {
					if (MorphUtils.isBannedBlock(st, entity) == null) {
				        MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(st, tg));
					}
				} else if (this.mode == Config.Mode.WHITELIST) {
					if (((List<String>)Config.getInstance().getValue("allowedBlocks")).contains(name)) {
						ac = " -";
					} else ac = " +";
					this.send(new ServerBoundConfigUpdatePacket("allowedBlocks", name + ac));
				} else if (this.mode == Config.Mode.BLACKLIST) {
					if (((List<String>)Config.getInstance().getValue("bannedBlocks")).contains(name)) {
						ac = " -";
					} else ac = " +";
					this.send(new ServerBoundConfigUpdatePacket("bannedBlocks", name + ac));
				}
				this.playDownSound();
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

	private void send(ServerBoundConfigUpdatePacket p) {
   	    MorphUtils.sendServer(p);
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

	private boolean needUnmorphBut() {
		return !this.isConfig() && MorphUtils.isBannedBlock(Blocks.AIR.defaultBlockState(), null) == null;
	}

	private boolean needFlameBut() {
		PlayerAccessor pl = (PlayerAccessor)this.entity;
		return !this.isConfig() && (pl.getBlockState(InPlayerBlockPos.ZERO).getBlock() instanceof TntBlock);
	}

	private boolean activeFlameBut() {
		PlayerAccessor pl = (PlayerAccessor)this.entity;
		return pl.getTnt() == null;
	}

	private void playDownSound() {
   	    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

	public boolean mouseReleased(double x, double y, int type) {
		if (type == 0) {
			this.scrollWork = false;
			CreativeModeTab tab = this.getTabAtPosition(x, y);
			if (tab != null) {
				this.selectTab(tab);
				return true;
			}
		}
		return super.mouseReleased(x, y, type);
	}

	public boolean mouseScrolled(double p_98527_, double p_98528_, double unkown, double p_98529_) {
		if (p_98529_ < 0 && this.scrollOff * 2 + 16 < list.size()) {
            this.scrollOff = this.scrollOff + 2;
        } else if (p_98529_ > 0 && this.scrollOff > 0) {
            this.scrollOff = this.scrollOff - 2;
        }
        return true; 
	}

	public boolean isPauseScreen() {
      return false;
    }

	public boolean charTyped(char c, int t) {
	  if (!init) {
	  	 init = true;
	  	 return false;
	  }
      return super.charTyped(c, t);
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
		this.searchBlock(this.searchBox.getValue());
	}

	@Override
	public void init() {
		super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
		this.refreshList();
		searchBox = new ListenerEditBox(this.font, this.leftPos + 99, this.topPos + -10, 70, 12, Component.literal("searchbox"), this::searchBlock);
		searchBox.setMaxLength(32767);
		searchBox.setBordered(false);
		searchBox.setTextColor(16777215);
		this.addRenderableWidget(searchBox);
		this.setInitialFocus(this.searchBox);
		searchBox.active = this.hasSearchBar();
		this.unmask = new SoftSpritedImageButton(this.leftPos + 10, this.topPos + this.imageHeight + 1, 26, 26, DEMORPH_BUT, e -> {
			 MorphUtils.sendServer(ServerBoundBlockMorphPacket.create(Blocks.AIR.defaultBlockState(), new CompoundTag()));
		});
		this.unmask.active = ((PlayerAccessor)this.entity).isFullActive();
		this.unmask.visible = this.needUnmorphBut();
		this.addRenderableWidget(this.unmask);
		this.fuse = new SoftSpritedImageButton(this.leftPos - 28, this.topPos + this.imageHeight + 1, 26, 26, FLAME_BUT, e -> {
			MorphUtils.sendServer(ServerBoundBlockMorphPacket.fuse());
		});
		this.fuse.active = this.activeFlameBut();
		this.fuse.visible = this.needFlameBut();
		this.addRenderableWidget(this.fuse);
		if (this.isConfig()) this.addRenderableWidget(Button.builder(Component.literal("<--"), b -> this.minecraft.setScreen(new ConfigScreen()) ).pos(this.leftPos + 10, this.topPos + this.imageHeight + 1).size(20, 20).build());
		if (pageCount > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.setPage(false)).pos(leftPos - 22,  topPos - 22).size(20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.setPage(true)).pos(leftPos + imageWidth, topPos - 22).size(20, 20).build());
        }
	}
}
