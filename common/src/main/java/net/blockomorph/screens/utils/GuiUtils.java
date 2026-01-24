package net.blockomorph.screens.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import net.blockomorph.screens.overlay.BlockHeartOverlay;
import net.blockomorph.screens.overlay.Overlay;
import net.blockomorph.screens.overlay.PlayerCrackOverlay;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class GuiUtils { //Cross-platform wrapper
	private static final Function<ResourceLocation, RenderType> GUI_TEXTURE_WITH_ALPHA = Util.memoize(ClientPlatformUtils.INSTANCE::bakeGuiShader);
	//alpha rendering fix for textures on this game version, because it's not fixed in vanilla                             /\
	private static final HashMap<Block, Boolean> BE_WITH_RENDERERS = new HashMap<>();
	public static final BlockPos AIR = new BlockPos(0, 500, 0);
	public static final Minecraft MC = Minecraft.getInstance();
	public static final List<Overlay> OVERLAYS = new ArrayList<>();
	private static final Vector3f DIFFUSE_LIGHT_START;
	private static final Vector3f DIFFUSE_LIGHT_END;
	private GuiGraphics GUI;
	private int mouseX;
	private int mouseY;
	private float tick;
	private Font font;

	static {
		Matrix4f matrix4f = (new Matrix4f()).scaling(1.0F, -1.0F, 1.0F).rotateYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float) Math.PI / 1.3F), 2.3561945F, 0.0F);
		DIFFUSE_LIGHT_START = matrix4f.transformDirection((new Vector3f(0.2F, 1.0F, -0.7F)).normalize(), new Vector3f());
		DIFFUSE_LIGHT_END = matrix4f.transformDirection((new Vector3f(-0.2F, 1.0F, 0.7F)).normalize(), new Vector3f());
		OVERLAYS.add(new PlayerCrackOverlay());
		OVERLAYS.add(new BlockHeartOverlay());
	}

	public static ResourceLocation res(String path) {
		return MorphUtils.res(path);
	}

	public static ResourceLocation vanillaRes(String path) {
		return MorphUtils.vanillaRes(path);
	}

	public void setGuiGraphics(GuiGraphics gui, Font font, int mouseX, int mouseY, float tick) {
		GUI = gui;
		this.font = font;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.tick = tick;
	}

	public GuiGraphics getGuiGraphics() {
		return GUI;
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
		u - start of texture X (left up corner)
		y - start of texture Y (left up corner)
		uvMaxX - length of start of UV
		uvMaxY - length of start of UV
		max X - length \
		max Y - height /   - size on screen
	*/
	public void blit(ResourceLocation texture, int x, int y, float u, float v, int uvMaxX, int uvMaxY, int maxX, int maxY) {
		this.drawPreparedTexture(texture, x, x + uvMaxX, y, y + uvMaxY, u / (float) maxX, (u + (float) uvMaxX) / (float) maxX, v / (float) maxY, (v + (float) uvMaxY) / (float) maxY);
	}

	public void blitMonoImage(ResourceLocation resourceLocation, int x, int y, int maxSizeX, int maxSizeY) {
		this.blit(resourceLocation, x, y, 0, 0, maxSizeX, maxSizeY, maxSizeX, maxSizeY);
	}

	private void drawPreparedTexture(ResourceLocation texture, int x, int xEnd, int y, int yEnd, float u, float uEnd, float v, float vEnd) {
		Matrix4f matrix4f = GUI.pose().last().pose();
		VertexConsumer vertexconsumer = GUI.bufferSource().getBuffer(GUI_TEXTURE_WITH_ALPHA.apply(texture));
		vertexconsumer.addVertex(matrix4f, (float) x, (float) y, 0).setUv(u, v);
		vertexconsumer.addVertex(matrix4f, (float) x, (float) yEnd, 0).setUv(u, vEnd);
		vertexconsumer.addVertex(matrix4f, (float) xEnd, (float) yEnd, 0).setUv(uEnd, vEnd);
		vertexconsumer.addVertex(matrix4f, (float) xEnd, (float) y, 0).setUv(uEnd, v);
		GUI.flush();
	}

	public void renderTooltip(Component text, int mouseX, int mouseY) {
		this.renderTooltip(List.of(text), mouseX, mouseY);
	}

	public void renderTooltip(List<Component> texts, int mouseX, int mouseY) {
		GUI.renderComponentTooltip(this.font, texts, mouseX, mouseY);
	}

	public void renderSprite(ResourceLocation resourceLocation, int x, int y, int maxSizeX, int maxSizeY) {
		GUI.blitSprite(resourceLocation, x, y, maxSizeX, maxSizeY);
	}

	public void renderFromSpriteClass(TextureAtlasSprite sprite, int x, int y, int maxSizeX, int maxSizeY, int color) {
		GUI.blit(x, y, 0, maxSizeX, maxSizeY, sprite,
				(float) FastColor.ARGB32.red(color) / 255,
				(float) FastColor.ARGB32.green(color) / 255,
				(float) FastColor.ARGB32.blue(color) / 255,
				(float) FastColor.ARGB32.alpha(color) / 255);
	}

	public void drawString(Component text, int x, int y, int color, boolean useShadow) {
		GUI.drawString(this.font, text, x, y, color, useShadow);
	}

	public void drawCenteredString(Component text, int xCenter, int y, int color, boolean useShadow) {
		int x = xCenter - this.font.width(text.getString()) / 2;
		this.drawString(text, x, y, color, useShadow);
	}

	public void drawCenteredStringWithAdditional(Component text, int xCenter, int y, int color, boolean useShadow, BiConsumer<Integer, Integer> additional) {
		int length = this.font.width(text.getString());
		int x = xCenter - length / 2;
		additional.accept(x, length);
		this.drawString(text, x, y, color, useShadow);
	}

	public void fill(int x, int y, int endX, int endY, int color) {
		GUI.fill(x, y, endX, endY, color);
	}

	public void blurScreen(int width, int height, int alpha) {
		this.fill(0, 0, width, height, FastColor.ARGB32.color(alpha, 77, 77, 77));
	}

	public void enableScrissors(int x, int y, int endX, int endY) {
		GUI.enableScissor(x, y, endX, endY);
	}

	public void disableScrissors() {
		GUI.disableScissor();
	}

	//HINT:   XY - upper left corner of item
	public void renderItem(ItemStack item, float x, float y, float scale, float zDepth) {
		if (scale == 1) scale = 16f;
		PoseStack pose = GUI.pose();
		pose.pushPose();

		pose.translate(x + 8, y + 8, 150 + zDepth);
		pose.scale(scale, -scale, scale);

		this.doMainRenderingItem(pose, item);

		pose.popPose();
	}

	public void renderInDepthIfNeededAfterBlockRendering(Runnable rendering) {
		PoseStack stack = GUI.pose();
		stack.pushPose();
		stack.translate(0, 0, 300);
		rendering.run();
		stack.popPose();
	}

	public static void renderOverlay(GuiGraphics gui, float delta) {
		GuiUtils guiUtils = new GuiUtils();
		guiUtils.setGuiGraphics(gui, MC.font, -100, -100, delta);
		Window window = Minecraft.getInstance().getWindow();
		if (MC.level != null) {
			OVERLAYS.forEach(overlay -> overlay.render(guiUtils, window.getGuiScaledWidth(), window.getGuiScaledHeight()));
		}
	}

	private void doMainRenderingItem(PoseStack stack, ItemStack item) {
		BakedModel bakedModel = MC.getItemRenderer().getModel(item, MC.level, MC.player, 0);
		boolean bl = !bakedModel.usesBlockLight();
		if (bl) {
			Lighting.setupForFlatItems();
		}

		MC.getItemRenderer().render(item, ItemDisplayContext.GUI, false, stack, GUI.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
		GUI.flush();
		if (bl) {
			Lighting.setupFor3DItems();
		}
	}

	//HINT:   XY - down corner of block
	public Throwable renderBlockInGui(BlockState blockState, @Nullable BlockEntity blockEntity, float x, float y, float scale) {
		PoseStack stack = GUI.pose();

		stack.pushPose();

		stack.translate(x, y, 100F);

		stack.scale(scale, -scale, scale);
		stack.mulPose(Axis.XP.rotationDegrees(30.0F));
		stack.mulPose(Axis.YP.rotationDegrees(-135F));

		Throwable reason = null;
		try {
			ClientPlatformUtils.INSTANCE.renderBlockInGui(GUI.bufferSource(), stack, blockState, blockEntity != null ? blockEntity.getBlockPos() : AIR);
		} catch (Throwable e) {
			reason = e;
		}
		RenderSystem.setShaderLights(DIFFUSE_LIGHT_START, DIFFUSE_LIGHT_END);
		try {
			this.renderBlockEntity(stack, blockEntity);
		} catch (Throwable e) {
			reason = e;
		}

		stack.popPose();
		return reason;
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

	public Throwable renderAdditionalOnBlock(BlockState blockState, float x, float y, float scale) {
		try {
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
		} catch (Throwable e) {
			return e;
		}
		return null;
	}

	private <T extends BlockEntity> void renderBlockEntity(PoseStack stack, T blockEntity) {
		if (blockEntity != null) {
			BlockEntityRenderer<T> renderer = MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
			if (renderer != null) {
				ClientLevelAccessor acc = ClientLevelAccessor.of(MC.level);
				try {
					acc.setSpecialRenderingMode(true);
					renderer.render(blockEntity, this.tick, stack, GUI.bufferSource(), LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
				} catch (Exception ignored) {
				} finally {
					acc.setSpecialRenderingMode(false);
				}
			}
		}
	}

	@Nullable
	public static String redirectBlockInfo(@Nullable Boolean fluid, HitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult hit) {
			if (MC.player != null && MC.player.getAbilities().instabuild) {
				return null;
			}
			Vec3 position = MorphUtils.getRealBlockPos(hit.getPlayer(), hit.getOffset());
			String x = formatCoordinate(position.x);
			String y = formatCoordinate(position.y);
			String z = formatCoordinate(position.z);
			if (fluid == null) return String.format(Locale.ROOT, "%s, %s, %s", x, y, z);
			return String.format(Locale.ROOT, "Targeted " + (fluid ? "Fluid" : "Block") + "%s, %s, %s", x, y, z);
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

	public static void pushHotbarMessage(Component text) {
		MC.gui.setOverlayMessage(text, false);
		MC.getNarrator().sayNow(text);
	}
}
