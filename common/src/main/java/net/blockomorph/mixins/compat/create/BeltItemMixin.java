package net.blockomorph.mixins.compat.create;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem")
public class BeltItemMixin {

	@Inject(method = "canConnect", at = @At("HEAD"), cancellable = true)
	private static void check(Level world, BlockPos first, BlockPos second, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(first.getX()) != InPlayerBlockPos.isMorphedPlayerX(second.getX())) {
			cir.setReturnValue(false);
		}
	}
}
