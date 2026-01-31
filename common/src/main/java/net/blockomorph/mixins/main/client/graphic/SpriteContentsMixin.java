package net.blockomorph.mixins.main.client.graphic;

import com.mojang.blaze3d.platform.NativeImage;
import net.blockomorph.utils.accessors.SpriteAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteAccessor {

	@Shadow @Final private NativeImage originalImage;

	@Override
	public NativeImage getImage() {
		return this.originalImage;
	}
}
