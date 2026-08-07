package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.misc.chairController.EntityChairController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPacketListenerMixin {

	@WrapOperation(method = {"handleMovePlayer", "tickPlayer"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z"))
	private boolean accept(ServerPlayer instance, Operation<Boolean> original) {
		if (instance instanceof EntityChairController.ChairedEntity ch && ch.getController().isActive())
			return true;
		return original.call(instance);
	}
}
