package net.blockomorph.mixins.main.system.morphState.restrictions.useMode;

import net.blockomorph.utils.config.enums.UseMode;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPacketListenerMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"), cancellable = true)
	public void checkAccess(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		if (UseMode.needRejectUse(this.player.level(), packet.getHitResult())) {
			ci.cancel();
		}
	}
}
