package net.blockomorph.mixins.compat.bbe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "betterblockentities.client.gui.config.ConfigCache")
public interface BBEconfigAccessor {

	@Accessor("masterOptimize")
	static void setMasterOptimize(boolean value) {
		throw new AssertionError();
	}

	@Accessor("masterOptimize")
	static boolean isOptimized() {
		throw new AssertionError();
	}
}