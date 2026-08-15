package net.blockomorph.screens.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.core.levelFlags.MorphedLevelFeatureFlags;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;

public class GuiUtils { //Cross-platform wrapper 	/\
	private static final HashMap<Block, Boolean> BE_WITH_RENDERERS = new HashMap<>();
	public static final BlockPos AIR = new BlockPos(0, 500, 0);
	public static final Minecraft MC = Minecraft.getInstance();
	public static final Vector3f DIFFUSE_LIGHT_START;
	public static final Vector3f DIFFUSE_LIGHT_END;
	private ItemStackRenderState scratchItemStackRenderState;
	private GuiGraphics guiGraphics;
	private int mouseX;
	private int mouseY;
	private float tick;
	private Font font;

	static {
		Matrix4f matrix4f = (new Matrix4f()).scaling(1.0F, -1.0F, 1.0F).rotateYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float) Math.PI / 1.3F), 2.3561945F, 0.0F);
		DIFFUSE_LIGHT_START = matrix4f.transformDirection((new Vector3f(0.2F, 1.0F, -0.7F)).normalize(), new Vector3f());
		DIFFUSE_LIGHT_END = matrix4f.transformDirection((new Vector3f(-0.2F, 1.0F, 0.7F)).normalize(), new Vector3f());
	}

	public static ResourceLocation res(String path) {
		return MorphUtils.res(path);
	}

	public static ResourceLocation vanillaRes(String path) {
		return MorphUtils.vanillaRes(path);
	}

	public void setGuiGraphics(GuiGraphics gui, Font font, int mouseX, int mouseY, float tick) {
		this.guiGraphics = gui;
		this.font = font;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.tick = tick;
		this.scratchItemStackRenderState = new ItemStackRenderState();
	}

	public GuiGraphics getGuiGraphics() {
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
	public void blit(ResourceLocation texture, int x, int y, float u, float v, int uvMaxX, int uvMaxY, int maxX, int maxY) {
		this.guiGraphics.blit(RenderType::guiTextured, texture, x, y, u, v, uvMaxX, uvMaxY, maxX, maxY);
	}

	public void blitMonoImage(ResourceLocation ResourceLocation, int x, int y, int maxSizeX, int maxSizeY) {
		this.blit(ResourceLocation, x, y, 0, 0, maxSizeX, maxSizeY, maxSizeX, maxSizeY);
	}

	public void renderTooltip(Component text, int mouseX, int mouseY) {
		this.renderTooltip(List.of(text), mouseX, mouseY);
	}

	public void renderTooltip(List<Component> texts, int mouseX, int mouseY) {
		this.guiGraphics.renderComponentTooltip(this.font, texts, mouseX, mouseY);
	}

	public void renderSprite(ResourceLocation resourceLocation, int x, int y, int maxSizeX, int maxSizeY) {
		this.guiGraphics.blitSprite(RenderType::guiTextured, resourceLocation, x, y, maxSizeX, maxSizeY);
	}

	public void renderFromSpriteClass(TextureAtlasSprite sprite, int x, int y, int maxSizeX, int maxSizeY, int color) {
		this.guiGraphics.blitSprite(RenderType::guiTextured, sprite, x, y, maxSizeX, maxSizeY, color);
	}

	public void drawString(Component text, int x, int y, int color, boolean useShadow) {
		this.guiGraphics.drawString(this.font, text, x, y, color, useShadow);
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

	//HINT:   XY - upper left corner of item
	public void renderItem(ItemStack item, float x, float y, float scale, float zDepth) {
		if (scale == 1) scale = 16f;
		PoseStack pose = this.guiGraphics.pose();
		pose.pushPose();

		MC.getItemModelResolver().updateForTopItem(this.scratchItemStackRenderState, item, ItemDisplayContext.GUI, MC.level, MC.player, 0);
		pose.translate(x + 8, y + 8, 150 + zDepth);
		pose.scale(scale, -scale, scale);

		this.doMainRenderingItem(pose);

		pose.popPose();
	}

	public void renderInDepthIfNeededAfterBlockRendering(Runnable rendering) {
		PoseStack stack = this.guiGraphics.pose();
		stack.pushPose();
		stack.translate(0, 0, 300);
		rendering.run();
		stack.popPose();
	}

	private void doMainRenderingItem(PoseStack stack) {
		boolean bl = !this.scratchItemStackRenderState.usesBlockLight();
		if (bl) {
			this.guiGraphics.flush();
			Lighting.setupForFlatItems();
		}

		this.guiGraphics.drawSpecial(buffer ->
				this.scratchItemStackRenderState.render(stack, buffer, 15728880, OverlayTexture.NO_OVERLAY));
		this.guiGraphics.flush();
		if (bl) {
			Lighting.setupFor3DItems();
		}
	}

	//HINT:   XY - down corner of block
	public Throwable renderBlockInGui(BlockState blockState, @Nullable BlockEntity blockEntity, float x, float y, float scale) {
		PoseStack stack = this.guiGraphics.pose();

		stack.pushPose();

		stack.translate(x, y, 100F);

		stack.scale(scale, -scale, scale);
		stack.mulPose(Axis.XP.rotationDegrees(30.0F));
		stack.mulPose(Axis.YP.rotationDegrees(-135F));

		Throwable reason = null;
		try {
			this.guiGraphics.drawSpecial(buffer ->
				this.renderBlockInGui(buffer, stack, blockState, blockEntity != null ? blockEntity.getBlockPos() : AIR)
			);
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

	private void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake) {//todo
		var level = MC.level;
		if (blockState.getRenderShape() == RenderShape.MODEL && level != null) {
			BlockStateModel model = MC.getModelManager().getBlockModelShaper().getBlockModel(blockState);
			LevelWithFlags acc = LevelWithFlags.of(level);
			acc.flags().customLightProvider = MorphedLevelFeatureFlags.LightProvider.ALWAYS_LIGHT;
			try {
				RandomSource random = RandomSource.create(blockState.getSeed(zeroOrFake));
				RenderType simplified = ItemBlockRenderTypes.getMovingBlockRenderType(blockState);
				MC.getBlockRenderer().getModelRenderer().tesselateBlock(MC.level,
						model.collectParts(random), blockState, zeroOrFake, stack, bufferSource.getBuffer(simplified), false, OverlayTexture.NO_OVERLAY);
			} finally {
				acc.flags().customLightProvider = null;
			}
		}
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
				LevelWithFlags level = LevelWithFlags.of(MC.level);
				try {
					Camera cam = Minecraft.getInstance().getBlockEntityRenderDispatcher().camera;
					level.flags().flywheelDisabled = true;
					this.guiGraphics.drawSpecial(buffer ->
							renderer.render(blockEntity, this.tick, stack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, cam.getPosition())
					);//TODO
				} catch (Exception ignored) {} finally {
					level.flags().flywheelDisabled = false;
				}
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
