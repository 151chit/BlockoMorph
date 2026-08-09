package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.accessors.CompatAccessors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension")
public interface SpriteContentsAcc extends CompatAccessors.SodiumSpriteActivator {
	@Shadow void sodium$setActive(boolean b);

	@Override
	default void activate$bm() {
		this.sodium$setActive(true);
	}
}
