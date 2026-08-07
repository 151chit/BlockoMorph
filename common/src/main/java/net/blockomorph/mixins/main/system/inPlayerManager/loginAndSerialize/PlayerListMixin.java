package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import net.blockomorph.network.ClientBoundSerializeInfoPacket;
import net.blockomorph.network.MorphNetwork;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Shadow @Final private MinecraftServer server;

	@Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
	public void respawnPlayer(ServerPlayer p_11237_, boolean p_11238_, Entity.RemovalReason p_348558_, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newPlayer) {
		BlockPosBounds.registerPlayer(newPlayer);
	}

	@Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addRespawnedPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void endRespawn(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newPlayer) {
		BlockPosBounds.refreshForRemoteClient(newPlayer, newPlayer, false);
		PlayerAccessor pl = PlayerAccessor.of(newPlayer);
		if (!keepAllPlayerData && pl.checkAndResetIfPlayerBroken()) {
			BlockInPlayer2 block = pl.getBlocksStorage().getFirst();
			if (block != null) {
				pl.setBlock(block.getOffset(), Blocks.AIR.defaultBlockState(), 3);
				pl.getManager().getNetworkManager().enqueueBlockNetworkUpdate(block.getOffset().asInt());
			}
		} else pl.sendAllContentToPlayer(newPlayer);
	}

	@Inject(method = "sendAllPlayerInfo", at = @At(value = "TAIL"))
	public void sendBoundedData(ServerPlayer player, CallbackInfo ci) {
		BlockPosBounds.registerPlayer(player);
		BlockPosBounds.refreshForRemoteClient(player, player, false);
		PlayerAccessor.of(player).sendAllContentToPlayer(player);
	}

	@Inject(require = 1, method = "remove", at= @At("TAIL"))
	public void disconnectPlayerAndRemoveTicks(ServerPlayer player, CallbackInfo ci) {
		PlayerWorldSerializer.freezeAndRunSaveProcess(player);
		BlockPosBounds.unregisterPlayer(player);
	}

	@Inject(method = "placeNewPlayer", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
	public void sendBlockPos(Connection connection, ServerPlayer player, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
		BlockPosBounds.refreshForRemoteClient(player, player, false);
		PlayerAccessor.of(player).sendAllContentToPlayer(player);
		MorphNetwork.sendAll(this.server, new ClientBoundSerializeInfoPacket(true, player));

		//config
		player.connection.send(new ClientBoundConfigUpdatePacket(Config.get()).toVanillaClientbound());
	}
}