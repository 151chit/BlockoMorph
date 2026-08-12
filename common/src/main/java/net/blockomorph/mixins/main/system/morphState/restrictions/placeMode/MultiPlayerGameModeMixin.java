package net.blockomorph.mixins.main.system.morphState.restrictions.placeMode;

import net.blockomorph.utils.config.enums.PlaceMode;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@ModifyVariable(method = "performUseItemOn", at = @At(value = "STORE"))
	private UseOnContext checkOutPlace(UseOnContext context) {
		return PlaceMode.getRealWorldPosIfOutPlaceMode(context);
	}

	@Inject(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
	public void checkDisabledPlace(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
		PlaceMode.rejectClickIfNoneValidPlaceMode(player, hand, blockHit, cir);
	}
}