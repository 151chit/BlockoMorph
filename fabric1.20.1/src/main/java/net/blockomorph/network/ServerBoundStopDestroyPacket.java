package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import net.fabricmc.fabric.api.networking.v1.PacketSender;

public class ServerBoundStopDestroyPacket extends FriendlyByteBuf {
   public ServerBoundStopDestroyPacket() {super(Unpooled.buffer());}

   public static void apply(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
		server.execute(() -> {
            if (MorphUtils.getEntityLookedAt(player, -1) instanceof PlayerAccessor mob) {
            	mob.removePlayer(player);
            }
		});
   }
   
}
