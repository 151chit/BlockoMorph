package net.blockomorph.mixins.compat.flywheel;

import net.blockomorph.utils.side.Side;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = {"dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl"}, remap = false)
public class Flywheel_BM_BackendMixin {

	@FastInject(method = "supportsVisualization", at = @At("HEAD"), require = 0, expect = 0)
	private static byte check(LevelAccessor world) {
		if (LevelWithFlags.of(world).flags().flywheelDisabled && Side.get() == Side.CLIENT) {
			return -1;
		}
		return 0;
	}
}
