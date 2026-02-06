package net.blockomorph.mixins.compat.flywheel;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {"dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl"}, remap = false)
public class Flywheel_BM_BackendMixin {

	@Inject(method = "supportsVisualization", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
	private static void check(LevelAccessor world, CallbackInfoReturnable<Boolean> cir) {
		if (world instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			cir.setReturnValue(false);
		}
	}
}
