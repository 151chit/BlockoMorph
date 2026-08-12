package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.compat.AtlasSpriteFinder;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements AtlasSpriteFinder.Holder {
	@Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;
	@Unique private AtlasSpriteFinder finder;

	@Inject(method = "upload", at = @At("HEAD"))
	private void release(SpriteLoader.Preparations preparations, CallbackInfo ci) {
		this.finder = null;
	}

	@Override
	public AtlasSpriteFinder getOrMake() {
		if (this.finder == null) {
			this.finder = new AtlasSpriteFinder(this.texturesByName);
		}
		return this.finder;
	}
}
