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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

public class ServerBoundInteractBlockPacket extends FriendlyByteBuf {
   public ServerBoundInteractBlockPacket(boolean click, int id, BlockPos pos) {
   	    super(Unpooled.buffer());
   	    writeBoolean(click);
   	    writeInt(id);
   	    writeBlockPos(pos);
   }

   public static void apply(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        boolean click = buf.readBoolean();
        int id = buf.readInt();
        BlockPos pos = buf.readBlockPos();
        
		server.execute(() -> {
            if (pos == null) return;
            if (click) {
            	Entity ent = ((ServerLevel)player.level()).getEntityOrPart(id);
            	if (id < 0 || !(ent instanceof Player)) return;
            	MorphUtils.onPlayerAttack(player, ent, pos);
            } else {
            	if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor mob) {
            		mob.removePlayer(pos, player);
            	}
            }
		});
   }
   
}
