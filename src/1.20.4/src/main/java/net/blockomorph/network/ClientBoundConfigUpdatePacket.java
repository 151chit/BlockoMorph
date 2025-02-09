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
import net.minecraft.network.protocol.common.custom.DiscardedPayload;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.fml.common.Mod;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record ClientBoundConfigUpdatePacket(Config c) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation(BlockomorphMod.MODID, "client_bound_config_update_packet");

   public ClientBoundConfigUpdatePacket(FriendlyByteBuf buffer) {
   	    this(Config.readFromBufer(buffer));
   }

   public void write(final FriendlyByteBuf buffer) {
   	    c.writeInBufer(buffer);
   }

   public static void apply(ClientBoundConfigUpdatePacket p, PlayPayloadContext context) {
   	if (context.flow() == PacketFlow.CLIENTBOUND) {
	  context.workHandler().submitAsync(() -> {
		if (Minecraft.getInstance().screen instanceof MorphScreen s) {
		    s.updateAllowed();
		}
	  }).exceptionally(e -> {
			context.packetHandler().disconnect(Component.literal(e.getMessage()));
			return null;
      });
   	}
   }

   @Override
   public ResourceLocation id() {
		return ID;
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID,
		ClientBoundConfigUpdatePacket::new,
		ClientBoundConfigUpdatePacket::apply);
   }
   
}
