package net.blockomorph.mixins.compat.frapi;

import net.blockomorph.utils.accessors.compat.FabricModelOnForge;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel")
public interface FabricModelMixin extends FabricModelOnForge {
	@Shadow boolean isVanillaAdapter();
	@Override
	default boolean isNotVanilla$blockomorph() {
		return !this.isVanillaAdapter();
	}
}
