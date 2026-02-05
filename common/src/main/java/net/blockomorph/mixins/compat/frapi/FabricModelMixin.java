package net.blockomorph.mixins.compat.frapi;

import net.blockomorph.utils.accessors.compat.FabricModelOnForge;
import net.blockomorph.utils.platform.FabricModelDetector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel")
public interface FabricModelMixin extends FabricModelOnForge {
	@Override
	default boolean isNotVanilla$blockomorph() {
		return FabricModelDetector.isFabricModel(this.getClass());
	}
}
