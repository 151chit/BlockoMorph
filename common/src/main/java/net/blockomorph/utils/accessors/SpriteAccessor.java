package net.blockomorph.utils.accessors;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;

public interface SpriteAccessor {

	NativeImage getImage();

	static SpriteAccessor of(SpriteContents contents) {
		return (SpriteAccessor) contents;
	}
}
