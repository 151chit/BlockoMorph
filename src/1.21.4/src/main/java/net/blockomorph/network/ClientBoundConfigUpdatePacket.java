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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.client.player.LocalPlayer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ClientBoundConfigUpdatePacket(Config c) implements CustomPacketPayload {
   public static final Type<ClientBoundConfigUpdatePacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MODID, "client_bound_config_update_packet"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundConfigUpdatePacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ClientBoundConfigUpdatePacket message) -> {
		message.c.writeInBufer(buffer);
	}, (RegistryFriendlyByteBuf buffer) -> new ClientBoundConfigUpdatePacket(Config.readFromBufer(buffer)));

   public static void apply(ClientBoundConfigUpdatePacket p, IPayloadContext context) {
   	if (context.flow() == PacketFlow.CLIENTBOUND) {
	  context.enqueueWork(() -> {
		if (Minecraft.getInstance().screen instanceof MorphScreen s) {
		   s.updateAllowed();
		}
	  });
   	}
   }

   @Override
   public Type<ClientBoundConfigUpdatePacket> type() {
		return ID;
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID,
		STREAM_CODEC,
		ClientBoundConfigUpdatePacket::apply);
   }
   
}
