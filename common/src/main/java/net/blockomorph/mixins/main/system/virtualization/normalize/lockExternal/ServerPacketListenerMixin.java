package net.blockomorph.mixins.main.system.virtualization.normalize.lockExternal;

import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {//prevent capture neighbor block from real world when clicking on morphed playerOwner
	@Shadow public ServerPlayer player;

	@Inject(method = "handleUseItemOn", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void redirectUpdateStart(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		LevelWithFlags.of(this.player.level()).flags().morphedBlockGetterDisabled = true;
	}

	@Inject(method = "handleUseItemOn", at = @At(ordinal = 1, shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void redirectUpdateEnd(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		LevelWithFlags.of(this.player.level()).flags().morphedBlockGetterDisabled = false;
	}

}