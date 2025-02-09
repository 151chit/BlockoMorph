package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.screens.*;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ClientBoundConfigUpdatePacket extends FriendlyByteBuf {
   public ClientBoundConfigUpdatePacket(Config c) {
   	    super(Unpooled.buffer());
   	    c.writeInBufer(this);
   }

   public static void apply(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
   	    Config.readFromBufer(buf);
		client.execute(() -> {
		  if (client.screen instanceof MorphScreen s) {
				s.updateAllowed();
		  }
		});
   }
   
}
