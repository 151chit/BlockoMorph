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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record ServerBoundStopDestroyPacket() implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation(BlockomorphMod.MODID, "server_bound_stop_destroy_packet");

   public ServerBoundStopDestroyPacket(FriendlyByteBuf buffer) {this();}

   public void write(final FriendlyByteBuf buffer) {}

   public static void handler(final ServerBoundStopDestroyPacket message, final PlayPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.workHandler().submitAsync(() -> {
			Player player = context.player().get();
            if (MorphUtils.getEntityLookedAt(player, -1) instanceof PlayerAccessor mob) {
            	mob.removePlayer(player);
            }
		  }).exceptionally(e -> {
				context.packetHandler().disconnect(Component.literal(e.getMessage()));
				return null;
		  });
		}
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID, 
		ServerBoundStopDestroyPacket::new, 
		ServerBoundStopDestroyPacket::handler);
  }

  @Override
  public ResourceLocation id() {
		return ID;
  }
}
