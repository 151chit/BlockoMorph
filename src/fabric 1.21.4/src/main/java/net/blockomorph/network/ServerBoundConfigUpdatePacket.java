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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ServerBoundConfigUpdatePacket(String option, String val) implements CustomPacketPayload {
   public static final Type<ServerBoundConfigUpdatePacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MOD_ID, "server_bound_config_update_packet"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundConfigUpdatePacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundConfigUpdatePacket message) -> {
		buffer.writeUtf(message.option);
   	    buffer.writeUtf(message.val);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundConfigUpdatePacket(buffer.readUtf(), buffer.readUtf()));

   public static void apply(ServerBoundConfigUpdatePacket m, ServerPlayer player) {
   	    String op = m.option;
   	    String val = m.val;
		if (player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig")) {
			Config.getInstance().parse(op, val, true);
		}
   }

   @Override
   public Type<ServerBoundConfigUpdatePacket> type() {
		return ID;
   }
   
}
