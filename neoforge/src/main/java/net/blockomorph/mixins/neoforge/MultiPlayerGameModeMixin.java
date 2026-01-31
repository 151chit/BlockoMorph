package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Inject(method = "performUseItemOn", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/neoforged/neoforge/common/util/TriState;isTrue()Z", remap = false), cancellable = true)
	public void checkAccessOnPlace(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		CommonPlatformUtils.onRightClick(localPlayer, interactionHand, blockHitResult, cir);
	}
}
