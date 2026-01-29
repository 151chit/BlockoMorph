package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class AttackSupportMixin {

	@Shadow @Nullable public HitResult hitResult;

	@Shadow public LocalPlayer player;

	@ModifyVariable(method = "continueAttack", at = @At("HEAD"))
	public boolean check(boolean needContinue) {
		if (needContinue && this.hitResult instanceof MorphedPlayerHitResult hit && hit.getPlayer().isFullActive()) {
			ConfigEnums.HitReaction hitReaction = Config.get().hitReaction.getValue();
			if (hitReaction != ConfigEnums.HitReaction.BRAKING) {
				return false;
			}
		}
		return needContinue;
	}
}
