package net.blockomorph.mixins.main.rawAccessors;

import com.mojang.blaze3d.platform.NativeImage;
import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteContents.class)
public class SpriteContentsAcc implements Accessors.SpriteAccessor {
	@Shadow @Final private NativeImage originalImage;
	@Override
	public NativeImage getImage$bm() {
		return this.originalImage;
	}
}
