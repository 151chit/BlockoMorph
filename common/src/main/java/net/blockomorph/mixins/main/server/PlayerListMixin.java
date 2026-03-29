package net.blockomorph.mixins.main.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.network.ClientBoundBlockPosBoundPacket;
import net.blockomorph.network.ClientBoundConfigUpdatePacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.BlockPosBounds;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.blockomorph.utils.tick.PlayersTickManager;
import net.blockomorph.utils.tick.PlayersTicks;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Unique private Vec3 tempPos;

	@Inject(method = "broadcast", at = @At(value = "HEAD"))
	public void redirect(Player p_11242_, double x, double y, double z, double p_11246_, ResourceKey<Level> p_11247_, Packet<?> p_11248_, CallbackInfo ci) {
		this.tempPos = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
	}

	@ModifyVariable(method = "broadcast", at = @At(value = "HEAD"), ordinal = 0)
	public double getX(double value) {
		return tempPos.x;
	}

	@ModifyVariable(method = "broadcast", at = @At(value = "HEAD"), ordinal = 1)
	public double getY(double value) {
		return tempPos.y;
	}

	@ModifyVariable(method = "broadcast", at = @At(value = "HEAD"), ordinal = 2)
	public double getZ(double value) {
		return tempPos.z;
	}


	@Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addRespawnedPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
	public void respawnPlayer(ServerPlayer p_11237_, boolean p_11238_, Entity.RemovalReason p_348558_, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newPlayer) {
		BlockPosBounds.onJoin(newPlayer);
		PlayerAccessor.of(newPlayer).sendAllContentToPlayer(newPlayer);
	}

	@Inject(method = "sendAllPlayerInfo", at = @At(value = "TAIL"))
	public void sendBoundedData(ServerPlayer player, CallbackInfo ci) {
		BlockPosBounds.onJoin(player);
		PlayerAccessor.of(player).sendAllContentToPlayer(player);
	}

	@Inject(method = "remove", at = @At("HEAD"))
	public void disconnectPlayer(ServerPlayer player, CallbackInfo ci) {
		if (player.isPassenger() && player.getVehicle() instanceof ServerPlayer) {
			player.stopRiding();
		}
	}

	@Inject(require = 1, method = "remove", at= @At("TAIL"))
	public void disconnectPlayerAndRemoveTicks(ServerPlayer serverPlayer, CallbackInfo ci) {
		for (PlayersTicks<?> ticks : PlayersTickManager.getTicks()) {
			ticks.stopAndUnload(serverPlayer);
		}
		PlayerMorphedSection.cleanProxyChunks(serverPlayer);
	}

	@Inject(method = "placeNewPlayer", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"))
	public void boundBlockPos(Connection connection, ServerPlayer player, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
		BlockPosBounds.boundPlayer(player);
		long gameTime = PlayersTickManager.getGameTime();
		for (PlayersTicks<?> playersTicks : PlayersTickManager.getTicks()) {
			playersTicks.replaceOrRegisterForPlayer(player, null, gameTime);
		}
	}

	@Inject(method = "placeNewPlayer", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
	public void sendBlockPos(Connection connection, ServerPlayer player, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
		PlayerMorphedSection pos = BlockPosBounds.getChunkPosForPlayer(player);
		if (pos != null) {
			MorphUtils.sendPlayer(new ClientBoundBlockPosBoundPacket(pos, player, false), player);
		}
	}

	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	public void sendPrimaryData(Connection connection, ServerPlayer player, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
		MorphUtils.sendPlayer(new ClientBoundConfigUpdatePacket(Config.get()), player);
		PlayerAccessor.of(player).sendAllContentToPlayer(player);
	}
}
