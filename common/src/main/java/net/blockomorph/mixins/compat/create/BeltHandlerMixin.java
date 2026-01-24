package net.blockomorph.mixins.compat.create;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler")
public class BeltHandlerMixin {

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getBlockPos()Lnet/minecraft/core/BlockPos;"), cancellable = true)
	private static void check(CallbackInfo ci, @Local(ordinal = 0) BlockPos first, @Local(ordinal = 0) HitResult result) {
		if (InPlayerBlockPos.isMorphedPlayerX(first.getX()) != InPlayerBlockPos.isMorphedPlayerX(result.getLocation().x)) {
			ci.cancel();
		}
	}
}
