package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ServerBoundInteractBlockPacket(boolean click) implements CustomPacketPayload {
   public static final Type<ServerBoundInteractBlockPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MODID, "server_bound_stop_destroy_packet"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundInteractBlockPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundInteractBlockPacket message) -> {
   	    buffer.writeBoolean(message.click);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundInteractBlockPacket(buffer.readBoolean()));

   public static void handler(final ServerBoundInteractBlockPacket message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.enqueueWork(() -> {
			Player player = context.player();
            if (MorphUtils.getEntityLookedAt(player, -1) instanceof PlayerAccessor mob) {
            	if (!message.click) {
            	    mob.removePlayer(player);
            	} else {
            		player.attack((Player)mob);
            	}
            }
		  }).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
		  });
		}
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID, 
		STREAM_CODEC, 
		ServerBoundInteractBlockPacket::handler);
  }

  @Override
  public Type<ServerBoundInteractBlockPacket> type() {
		return ID;
  }
}
