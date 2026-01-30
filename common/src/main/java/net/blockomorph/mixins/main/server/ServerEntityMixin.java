package net.blockomorph.mixins.main.server;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundBlockPosBoundPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ServerEntityAccessor;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.dataSyncher.SyncedEntity;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Supplier;

@Mixin(ServerEntity.class)
public class ServerEntityMixin implements ServerEntityAccessor {
	@Shadow @Final private Entity entity;
	@Shadow @Final private ServerLevel level;
	@Shadow private int teleportDelay;
	@Unique
	private Supplier<Set<ServerPlayerConnection>> seenBy$blockomorph;

	public void assignSeenBy$blockomorph(Supplier<Set<ServerPlayerConnection>> seenBy) {
		this.seenBy$blockomorph = seenBy;
	}

	@Inject(method = "sendChanges", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.AFTER, ordinal = 0))
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
			if (this.seenBy$blockomorph != null) {
				for (ServerPlayerConnection connection : this.seenBy$blockomorph.get()) {
					if (connection.getPlayer().position().distanceToSqr(this.entity.position()) <= 1.5d) {
						this.teleportDelay = 10_000_000;
						break;
					}
				}
				return;
			}
			for (ServerPlayer player : this.level.getChunkSource().chunkMap.getPlayers(this.entity.chunkPosition(), false)) {
				if (player.position().distanceToSqr(this.entity.position()) <= 1.5d) {
					this.teleportDelay = 10_000_000;
					break;
				}
			}
		}
	}

	@Inject(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPassengers()Ljava/util/List;"))
	public void sync(CallbackInfo ci) {
		SyncedEntity.of(this.entity).checkOrSendImmediate(this::sendCustomPacket, false);
	}

	private void sendCustomPacket(BlockMorphPacket packet) {
		if (this.seenBy$blockomorph != null) {
			if (this.entity instanceof ServerPlayer player)
				MorphUtils.sendPlayer(packet, player);
			for (ServerPlayerConnection connection : this.seenBy$blockomorph.get()) {
				MorphUtils.sendPlayer(packet, connection.getPlayer());
			}
			return;
		}
		this.level.getChunkSource().chunkMap.getPlayers(this.entity.chunkPosition(), false).forEach(player -> {
			MorphUtils.sendPlayer(packet, player);
		});
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
