package net.blockomorph.mixins.main.suppressVanilla.deathAnim;

import com.mojang.authlib.GameProfile;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends Player implements PlayerAccessor {

	public LocalPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@FastInject(method = "tickDeath", at = @At("HEAD"))
	private boolean suppress() {
		if (this.isBlockomorphActive()) {
			this.remove(RemovalReason.KILLED);
			return false;
		}
		return true;
	}
}
