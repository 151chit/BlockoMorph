package net.blockomorph.screens.utils;

import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.screens.PlatformGuiService;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.Accessors;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

public class GuiUtils { //Cross-platform wrapper 	/\
	private static final HashMap<Block, Boolean> BE_WITH_RENDERERS = new HashMap<>();
	public static final BlockPos AIR = new BlockPos(0, 500, 0);
	public static final Minecraft MC = Minecraft.getInstance();
	public static final Vector3f DIFFUSE_LIGHT_START;
	public static final Vector3f DIFFUSE_LIGHT_END;
	private GuiGraphicsExtractor guiGraphics;
	private int mouseX;
	private int mouseY;
	private float tick;
	private Font font;

	static {
		Matrix4f matrix4f = (new Matrix4f()).scaling(1.0F, -1.0F, 1.0F).rotateYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float) Math.PI / 1.3F), 2.3561945F, 0.0F);
		DIFFUSE_LIGHT_START = matrix4f.transformDirection((new Vector3f(0.2F, 1.0F, -0.7F)).normalize(), new Vector3f());
		DIFFUSE_LIGHT_END = matrix4f.transformDirection((new Vector3f(-0.2F, 1.0F, 0.7F)).normalize(), new Vector3f());
	}

	public static Identifier res(String path) {
		return MorphUtils.res(path);
	}

	public static Identifier vanillaRes(String path) {
		return MorphUtils.vanillaRes(path);
	}

	public void setGuiGraphics(GuiGraphicsExtractor gui, Font font, int mouseX, int mouseY, float tick) {
		this.guiGraphics = gui;
		this.font = font;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.tick = tick;
	}

	public GuiGraphicsExtractor getGuiGraphics() {
		return this.guiGraphics;
	}

	public Font getFont() {
		return font;
	}

	public int getMouseX() {
		return mouseX;
	}

	public int getMouseY() {
		return mouseY;
	}

	public float getTick() {
		return tick;
	}

	/* HINT:
		X - up left corner
		Y - up left corner
		u - start ofObj texture X (left up corner)
		y - start ofObj texture Y (left up corner)
		uvMaxX - length ofObj start ofObj UV
		uvMaxY - length ofObj start ofObj UV
		max X - length \
		max Y - height /   - size on screen
	*/
	public void blit(Identifier texture, int x, int y, float u, float v, int uvMaxX, int uvMaxY, int maxX, int maxY) {
		this.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, uvMaxX, uvMaxY, maxX, maxY);
	}

	public void blitMonoImage(Identifier Identifier, int x, int y, int maxSizeX, int maxSizeY) {
		this.blit(Identifier, x, y, 0, 0, maxSizeX, maxSizeY, maxSizeX, maxSizeY);
	}

	public void renderTooltip(Component text, int mouseX, int mouseY) {
		this.renderTooltip(List.of(text), mouseX, mouseY);
	}

	public void renderTooltip(List<Component> texts, int mouseX, int mouseY) {
		this.guiGraphics.setComponentTooltipForNextFrame(this.font, texts, mouseX, mouseY);
	}

	public void renderSprite(Identifier Identifier, int x, int y, int maxSizeX, int maxSizeY) {
		this.guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier, x, y, maxSizeX, maxSizeY);
	}

	public void renderFromSpriteClass(TextureAtlasSprite sprite, int x, int y, int maxSizeX, int maxSizeY, int color) {
		this.guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, maxSizeX, maxSizeY, color);
	}

	public void drawString(Component text, int x, int y, int color, boolean useShadow) {
		if (ARGB.alpha(color) == 0) {
			color = (255 << 24) | color;
		}
		this.guiGraphics.text(this.font, text, x, y, color, useShadow);
	}

	public void drawCenteredString(Component text, int xCenter, int y, int color, boolean useShadow) {
		int x = xCenter - this.font.width(text.getString()) / 2;
		this.drawString(text, x, y, color, useShadow);
	}

	public void fill(int x, int y, int endX, int endY, int color) {
		this.guiGraphics.fill(x, y, endX, endY, color);
	}

	public void blurScreen(int width, int height, int alpha) {
		this.fill(0, 0, width, height, ARGB.color(alpha, 77, 77, 77));
	}

	public void enableScissors(int x, int y, int endX, int endY) {
		this.guiGraphics.enableScissor(x, y, endX, endY);
	}

	public void disableScissors() {
		this.guiGraphics.disableScissor();
	}

	//HINT:   XY - upper left corner ofObj item
	public void renderItem(ItemStack item, float x, float y, float scale, float ignored) {
		if (scale == 1) scale = 16f;

		TrackingItemStackRenderState trackingItemStackRenderState = new DynamicSizeItemStackRenderState(scale);
		MC.getItemModelResolver().updateForTopItem(trackingItemStackRenderState, item, ItemDisplayContext.GUI, MC.level, MC.player, 0);
		Accessors.GuiGraphicsAccessor.of(this.guiGraphics).getGuiRenderState$bm().addItem(
				new GuiItemRenderState(new Matrix3x2f(this.guiGraphics.pose()), trackingItemStackRenderState, (int) x, (int) y,
						PlatformGuiService.INSTANCE.scissorsPeek(this.guiGraphics)));
	}

	public void renderInDepthIfNeededAfterBlockRendering(Runnable rendering) {
		rendering.run();
	}

	//HINT:   XY - down corner ofObj block
	public void renderBlockInGui(BlockState blockState, @Nullable BlockEntity blockEntity, float x, float y, float scale) {
		PlatformGuiService.INSTANCE.submitCustomPipRenderState(this.guiGraphics,
				new GuiBlockRenderState(blockState, blockEntity, (int) x, (int) y, scale, this.tick, PlatformGuiService.INSTANCE.scissorsPeek(this.guiGraphics)));
	}

	private Boolean skipCheckOrContainsRenderer(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof EntityBlock entityBlock) {
			return BE_WITH_RENDERERS.computeIfAbsent(block, b -> {
				BlockEntity ent = entityBlock.newBlockEntity(AIR, state);
				if (ent == null) return false;
				return MC.getBlockEntityRenderDispatcher().getRenderer(ent) != null;
			});
		}
		return null;
	}

	public void renderAdditionalOnBlock(BlockState blockState, float x, float y, float scale) {
		Boolean result = this.skipCheckOrContainsRenderer(blockState);
		if (blockState.getRenderShape() == RenderShape.INVISIBLE && (result == null || !result)) {
			Item item = null;
			if (blockState.getBlock() instanceof LiquidBlock) {
				item = blockState.getFluidState().getType().getBucket();
			} else if (blockState.getBlock().asItem() != Items.AIR) {
				item = blockState.getBlock().asItem();
			}
			if (item != null) {
				ItemStack itemStack = new ItemStack(item);
				Map<String, String> map = new HashMap<>();
				for (Property<?> property : blockState.getProperties()) {
					map.put(property.getName(), blockState.getValue(property).toString());
				}
				itemStack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(map));
				this.renderItem(itemStack, x, y, scale, 100);
			}
		}
	}

	@Nullable
	public static String hideMorphedBlocksKeyPos(@Nullable Boolean fluid, HitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult hit) {
			if (MC.player != null && MC.player.getAbilities().instabuild) return null;
			String x = formatCoordinate(MorphMath.getRealBlockPosAxis(Direction.Axis.X, hit.getBlock()));
			String y = formatCoordinate(MorphMath.getRealBlockPosAxis(Direction.Axis.Y, hit.getBlock()));
			String z = formatCoordinate(MorphMath.getRealBlockPosAxis(Direction.Axis.Z, hit.getBlock()));
			if (fluid == null) return String.format(Locale.ROOT, "%s, %s, %s", x, y, z);
			return String.format(Locale.ROOT, "Targeted " + (fluid ? "Fluid" : "Block") + ": %s, %s, %s", x, y, z);
		}
		return null;
	}

	private static String formatCoordinate(double coordinate) {
		if (coordinate == (int) coordinate) {
			return String.valueOf((int) coordinate);
		} else {
			return String.format(Locale.ROOT, "%.3f", coordinate);
		}
	}

	public static boolean isMouseOver(int x, int y, int endX, int endY, double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= endX && mouseY >= y && mouseY < endY;
	}

	public static boolean isInBounds(Rect2i box, double mouseX, double mouseY) {
		return isMouseOver(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), mouseX, mouseY);
	}

	public static void playClickSound() {
		MC.getSoundManager().play(getClickSound());
	}

	public static SoundInstance getClickSound() {
		return SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f);
	}
}
