package net.blockomorph.utils.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockomorph.screens.utils.FogLiquidModifier;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.SpriteAccessor;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.ServiceLoader;

public interface ClientPlatformUtils {
	ClientPlatformUtils INSTANCE = ServiceLoader.load(ClientPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load client platform-depended utils, mod cannot run!"));

	void collectItemsFromAllTabs(CreativeModeTab tab, ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output, CreativeModeTab.DisplayItemsGenerator orig);
	default FogLiquidModifier.LiquidFogData calculateData(Level lv, BlockInPlayer2 block) {
		return calculateAutomatic(lv, block);
	}

	@Nullable TextureAtlasSprite[] getPlatformFluidSprite(Level lv, BlockInPlayer2 block);
	Integer getPlatformFluidTint(Level lv, BlockInPlayer2 block);
	void renderBlockInWorld(boolean translucent, PoseStack posestack, MultiBufferSource buffer, PlayerAccessor pl, BlockInPlayer2 block, RandomSource randomSource);
	void renderBlockInGui(MultiBufferSource bufferSource, PoseStack stack, BlockState blockState, BlockPos zeroOrFake);
	void renderBrake(PoseStack posestack, VertexConsumer buffer, PlayerAccessor pl, BlockInPlayer2 block, RandomSource randomSource);
	boolean hasSearchBarInTab(CreativeModeTab tab);
	RenderType bakeGuiShader(ResourceLocation texture);
	void addAdditionalData(TerrainParticle particle, BlockPos keyPos, BlockState state);
	boolean isValidDestroyBlock(Level level, BlockPos keyPos, BlockState state, ParticleEngine engine);

	static FogLiquidModifier.LiquidFogData calculateAutomatic(Level lv, BlockInPlayer2 block) {
		try {
			Map.Entry<TextureAtlasSprite[], Integer> info = getPlatformFluidTexture(lv, block);
			if (info.getKey() != null) {
				int argbPixel = getArgbPixel(info);
				float start = -8;
				float end = 36;
				if (FastColor.ARGB32.alpha(argbPixel) > 235) {
					start = 0.25f;
					end = 1;
				}
				if (info.getValue() != null) argbPixel = FastColor.ARGB32.lerp(0.5f, argbPixel, info.getValue());
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
		int argbPixel = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2);
		argbPixel = ((argbPixel >> 24) & 0xFF) << 24 |
				((argbPixel) & 0xFF) << 16 |
				((argbPixel >> 8) & 0xFF) << 8 |
				((argbPixel >> 16) & 0xFF);
		return argbPixel;
	}

	private static Map.Entry<TextureAtlasSprite[], Integer> getPlatformFluidTexture(Level lv, BlockInPlayer2 block) {
		int tint = INSTANCE.getPlatformFluidTint(lv, block);
		TextureAtlasSprite[] atextureatlassprite = INSTANCE.getPlatformFluidSprite(lv, block);
		return new AbstractMap.SimpleEntry<>(atextureatlassprite, tint == -1 || tint == 16777215 ? null : tint);
	}
}
