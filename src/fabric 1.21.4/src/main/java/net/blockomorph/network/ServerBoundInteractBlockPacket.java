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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public record ServerBoundInteractBlockPacket(boolean click, int id, BlockPos pos) implements CustomPacketPayload {
   public static final Type<ServerBoundInteractBlockPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MOD_ID, "server_bound_stop_destroy_packet"));
   
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundInteractBlockPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundInteractBlockPacket message) -> {
   	    buffer.writeBoolean(message.click);
   	    buffer.writeInt(message.id);
   	    buffer.writeBlockPos(message.pos);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundInteractBlockPacket(buffer.readBoolean(), buffer.readInt(), buffer.readBlockPos()));

   public static void apply(final ServerBoundInteractBlockPacket message, Player player) {
   	        if (message.pos == null) return;
            if (message.click) {
            	Entity ent = ((ServerLevel)player.level()).getEntityOrPart(message.id);
            	if (message.id < 0 || !(ent instanceof Player)) return;
            	MorphUtils.onPlayerAttack(player, ent, message.pos);
            } else {
            	if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor mob) {
            		mob.removePlayer(message.pos, player);
            	}
            }
   }

   @Override
   public Type<ServerBoundInteractBlockPacket> type() {
    	return ID;
   }
   
}
