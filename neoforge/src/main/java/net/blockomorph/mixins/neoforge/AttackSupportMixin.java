package net.blockomorph.mixins.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class AttackSupportMixin {
	@Shadow @Nullable public HitResult hitResult;
	@Shadow public MultiPlayerGameMode gameMode;
	@Shadow public LocalPlayer player;

	@Inject(method = "startAttack", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"), cancellable = true)
	public void attack(CallbackInfoReturnable<Boolean> cir, @Local InputEvent.InteractionKeyMappingTriggered event) {
		if (this.hitResult instanceof MorphedPlayerHitResult hit && hit.getBlock().getPlayer().isBlockomorphFullActive()) {
			ConfigEnums.HitReaction hitReaction = Config.get().hitReaction.getValue();
			boolean needSwing = false;
			if (hitReaction != ConfigEnums.HitReaction.BRAKING) {
				cir.setReturnValue(true);
				needSwing = true;
			}
			if (hitReaction.hand) {
				this.gameMode.attack(this.player, hit.getBlock().getPlayer().player());
			} else if (hitReaction != ConfigEnums.HitReaction.BRAKING) {
				this.player.resetAttackStrengthTicker();
			}
			if (needSwing && event.shouldSwingHand()) this.player.swing(InteractionHand.MAIN_HAND);
		}
	}
}
