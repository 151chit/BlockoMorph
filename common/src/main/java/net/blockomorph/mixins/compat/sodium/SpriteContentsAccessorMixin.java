package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.accessors.compat.SpriteRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension")
public interface SpriteContentsAccessorMixin extends SpriteRunner {
	@Shadow void sodium$setActive(boolean b);

	@Override
	default void run$blockomorph() {
		this.sodium$setActive(true);
	}
}
