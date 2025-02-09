package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.config.*;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;
import io.netty.buffer.Unpooled;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

public class ServerBoundConfigUpdatePacket extends FriendlyByteBuf {
   public ServerBoundConfigUpdatePacket(String option, String val) {
   	    super(Unpooled.buffer());
   	    writeUtf(option);
   	    writeUtf(val);
   }

   public static void apply(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buffer, PacketSender responseSender) {
   	    String op = buffer.readUtf();
   	    String val = buffer.readUtf();
		server.execute(() -> {
		  if (player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig")) {
				Config.getInstance().parse(op, val, true);
		  }
		});
   }
   
}
