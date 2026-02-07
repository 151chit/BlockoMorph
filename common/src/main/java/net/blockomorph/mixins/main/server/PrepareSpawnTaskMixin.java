package net.blockomorph.mixins.main.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.tick.PlayersTickManager;
import net.blockomorph.utils.tick.PlayersTicks;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public class PrepareSpawnTaskMixin {

	@Inject(require = 1, method = "spawn", at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;problemPath()Lnet/minecraft/util/ProblemReporter$PathElement;"))
	private void boundBlockPos(Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfoReturnable<ServerPlayer> cir, @Local ServerPlayer player) {
		BlockPosBounds.boundPlayer(player);
		long gameTime = PlayersTickManager.getGameTime();
		for (PlayersTicks<?> playersTicks : PlayersTickManager.getTicks()) {
			playersTicks.replaceOrRegisterForPlayer(player, null, gameTime);
		}
	}
}
