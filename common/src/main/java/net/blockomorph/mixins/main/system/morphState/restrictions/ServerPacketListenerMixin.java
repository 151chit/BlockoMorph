package net.blockomorph.mixins.main.system.morphState.restrictions;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.enums.ConfigEnums;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {

	@Shadow public ServerPlayer player;

	@Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"), cancellable = true)
	public void checkAccess(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(packet.getHitResult().getBlockPos());
		if (pl != null && !pl.isBlockomorphFullActive()) {
			ci.cancel();
		}
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "handleInteract", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
	public void checkAccess(ServerboundInteractPacket packet, CallbackInfo ci) {
		Entity entity = this.player.level().getEntityOrPart(packet.entityId());
		if (entity instanceof PlayerAccessor pl && pl.isBlockomorphActive()) {
			if (!Config.get().hitReaction.getValue().hand) ci.cancel();
		}
	}

	@Inject(method = "handlePlayerAction", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
	public void checkAccess(ServerboundPlayerActionPacket pkt, CallbackInfo ci) {
		switch (pkt.getAction()) {
			case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK: {
				PlayerAccessor pl = InPlayerBlockPos.findPlayer(pkt.getPos());
				if (pl != null) {
					if (!pl.isBlockomorphFullActive() || Config.get().hitReaction.getValue() != ConfigEnums.HitReaction.BRAKING) {
						ci.cancel();
					}
				}
			}
		}
	}
}