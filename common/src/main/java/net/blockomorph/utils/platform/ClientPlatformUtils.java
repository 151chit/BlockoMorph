package net.blockomorph.utils.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.KeyMappings;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.VertexRecorder;
import net.blockomorph.utils.accessors.SpriteAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Supplier;

public interface ClientPlatformUtils {
	Supplier<Minecraft> MC = () -> GuiUtils.MC;
	ClientPlatformUtils INSTANCE = ServiceLoader.load(ClientPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load client platform-depended utils, mod cannot run!"));

	void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, OutPutBm output, CreativeModeTab.DisplayItemsGenerator orig);
	void fallbackVanillaTabContent(CreativeModeTab.ItemDisplayParameters parameters, OutPutBm outPutBm, CreativeModeTab.DisplayItemsGenerator content);
	interface OutPutBm {
		void accept(ItemStack item, CreativeModeTab.TabVisibility tabVisibility);
	}


	default FogLiquidModifier.LiquidFogData calculateData(Level lv, BlockInPlayer2 block) {
		return calculateAutomatic(lv, block);
	}

	default @Nullable TextureAtlasSprite[] getPlatformFluidSprite(BlockAndTintGetter lv, BlockState state, BlockPos keyPos) {
		FluidState fluidState = state.getFluidState();
		var fluidModel = MC.get().getModelManager().getFluidStateModelSet().get(fluidState);
		TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
		sprites[0] = this.getSpriteOrNull(fluidModel.stillMaterial());
		sprites[1] = this.getSpriteOrNull(fluidModel.flowingMaterial());
		sprites[2] = this.getSpriteOrNull(fluidModel.overlayMaterial());
		return sprites;
	}

	private TextureAtlasSprite getSpriteOrNull(Material.Baked baked) {
		return baked != null ? baked.sprite() : null;
	}

	default Integer getPlatformFluidTint(BlockAndTintGetter lv, BlockState state, BlockPos keyPos) {
		FluidState fluidState = state.getFluidState();
		var tint = MC.get().getModelManager().getFluidStateModelSet().get(fluidState).tintSource();
		if (tint != null) {
			return tint.colorInWorld(state, lv, keyPos);
		}
		return null;
	}

	static RenderType layerToRenderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderTypes.solidMovingBlock();
			case CUTOUT -> RenderTypes.cutoutMovingBlock();
			case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
		};
	}

	boolean hasSearchBarInTab(CreativeModeTab tab);
	void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state);
	boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine);
	@Nullable ScreenRectangle scissorsPeek(GuiGraphicsExtractor GuiGraphicsExtractor);
	void submitCustomPipRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, PictureInPictureRenderState renderState);

	static FogLiquidModifier.LiquidFogData calculateAutomatic(Level lv, BlockInPlayer2 block) {
		try {
			Map.Entry<TextureAtlasSprite[], Integer> info = getPlatformFluidTexture(lv, block);
			if (info.getKey() != null) {
				int argbPixel = getArgbPixel(info);
				float start = -8;
				float end = 36;
				if (ARGB.alpha(argbPixel) > 235) {
					start = 0.25f;
					end = 1;
				}
				if (info.getValue() != null) argbPixel = ARGB.linearLerp(0.5f, argbPixel, info.getValue());
				return new FogLiquidModifier.LiquidFogData(start, end, argbPixel);
			}
		} catch (Throwable ignored) {
			return null;
		}
		return null;
	}

	private static int getArgbPixel(Map.Entry<TextureAtlasSprite[], Integer> info) {
		TextureAtlasSprite sprite = info.getKey()[0];
		NativeImage image = SpriteAccessor.of(sprite.contents()).getImage();
		return image.getPixel(image.getWidth() / 2, image.getHeight() / 2);
	}

	private static Map.Entry<TextureAtlasSprite[], Integer> getPlatformFluidTexture(Level lv, BlockInPlayer2 block) {
		int tint = INSTANCE.getPlatformFluidTint((BlockAndTintGetter) lv, block.getBlockState(), block.getPos());
		TextureAtlasSprite[] atextureatlassprite = INSTANCE.getPlatformFluidSprite((BlockAndTintGetter) lv, block.getBlockState(), block.getPos());
		return new AbstractMap.SimpleEntry<>(atextureatlassprite, tint == -1 || tint == 16777215 ? null : tint);
	}

	@FunctionalInterface
	interface BlockCrackingCheck {
		boolean check(BlockState blockState);
	}

	static void crackBlock(ClientLevel level, BlockPos keyPos, Direction dir, BlockCrackingCheck platformDependedCheck, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerX(keyPos.getX())) {
			BlockState blockstate = level.getBlockState(keyPos);
			Vec3 realPos = InPlayerBlockPos.checkOnReal(Vec3.atLowerCornerOf(keyPos));
			ci.cancel();
			if (blockstate.getRenderShape() != RenderShape.INVISIBLE && platformDependedCheck.check(blockstate)) {
				double i = realPos.x;
				double j = realPos.y;
				double k = realPos.z;
				float f = 0.1F;
				double d = 0.2D;
				AABB aabb = blockstate.getShape(level, keyPos).bounds();
				RandomSource randomSource = level.getRandom();
				double d0 = i + randomSource.nextDouble() * (aabb.maxX - aabb.minX - d) + f + aabb.minX;
				double d1 = j + randomSource.nextDouble() * (aabb.maxY - aabb.minY - d) + f + aabb.minY;
				double d2 = k + randomSource.nextDouble() * (aabb.maxZ - aabb.minZ - d) + f + aabb.minZ;
				if (dir == Direction.DOWN) {
					d1 = j + aabb.minY - f;
				} else if (dir == Direction.UP) {
					d1 = j + aabb.maxY + f;
				} else if (dir == Direction.NORTH) {
					d2 = k + aabb.minZ - f;
				} else if (dir == Direction.SOUTH) {
					d2 = k + aabb.maxZ + f;
				} else if (dir == Direction.WEST) {
					d0 = i + aabb.minX - f;
				} else if (dir == Direction.EAST) {
					d0 = i + aabb.maxX + f;
				}

				TerrainParticle particle = new TerrainParticle(level, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate, BlockPos.containing(realPos));
				INSTANCE.addAdditionalData(particle, keyPos, blockstate);
				Minecraft.getInstance().particleEngine.add(particle.setPower(0.2F).scale(0.6F));
			}
		}
	}
}
