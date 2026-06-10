package net.blockomorph.mixins.main.server;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundBlockPosBoundPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.TrackedEntityAccessor;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.dataSyncher.SyncedEntity;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
	@Shadow @Final private Entity entity;
	@Shadow private int teleportDelay;
	@Shadow @Final private ServerEntity.Synchronizer synchronizer;

	@Inject(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersFiltered(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V", shift = At.Shift.AFTER, ordinal = 0))
	private void fixAsync(CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer pl) {
			pl.connection.send(new ClientboundSetPassengersPacket(pl));
		}
	}

	@Inject(method = "addPairing", at = @At(value = "TAIL"))
	public void start(ServerPlayer looker, CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer pl && this.entity instanceof PlayerAccessor acc) {
			PlayerMorphedSection pos = BlockPosBounds.getChunkPosForPlayer(pl);
			if (pos != null) {
				MorphUtils.sendPlayer(new ClientBoundBlockPosBoundPacket(pos, pl, false), looker);
			}
			acc.sendAllContentToPlayer(looker);
		}
		SyncedEntity.of(this.entity).checkOrSendImmediate(this::sendCustomPacket, true);
	}

	/**
	 * CHECK THIS INJECTION AFTER VERSION UPDATE!!!
	 */
	@Inject(require = 1, method = "sendChanges", at = @At(ordinal = 1, opcode = Opcodes.GETFIELD, value = "FIELD", target = "Lnet/minecraft/server/level/ServerEntity;teleportDelay:I"))
	private void changeSyncStrategy(CallbackInfo ci) {
		if (this.entity instanceof PlayerAccessor acc && acc.isFullActive()) {
			this.teleportDelay = 10_000_000;
		} else {
			if (this.synchronizer instanceof TrackedEntityAccessor acc) {
				for (ServerPlayerConnection connection : acc.getSeenBy$blockomorph()) {
					if (connection.getPlayer().position().distanceToSqr(this.entity.position()) <= 1.5d) {
						this.teleportDelay = 10_000_000;
						break;
					}
				}
			}
		}
	}

	@Inject(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPassengers()Ljava/util/List;"))
	public void sync(CallbackInfo ci) {
		SyncedEntity.of(this.entity).checkOrSendImmediate(this::sendCustomPacket, false);
	}

	private void sendCustomPacket(BlockMorphPacket packet) {
		if (this.synchronizer instanceof TrackedEntityAccessor acc) {
			if (this.entity instanceof ServerPlayer player)
				MorphUtils.sendPlayer(packet, player);
			for (ServerPlayerConnection connection : acc.getSeenBy$blockomorph()) {
				MorphUtils.sendPlayer(packet, connection.getPlayer());
			}
		}
	}

	@Inject(method = "removePairing", at = @At(value = "TAIL"))
	public void stop(ServerPlayer looker, CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer pl) {
			PlayerMorphedSection pos = BlockPosBounds.getChunkPosForPlayer(pl);
			if (pos != null) {
				MorphUtils.sendPlayer(new ClientBoundBlockPosBoundPacket(pos, pl, true), looker);
			}
		}
	}
}
