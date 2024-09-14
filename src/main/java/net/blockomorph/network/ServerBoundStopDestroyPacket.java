package net.blockomorph.network;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.fml.common.Mod;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerBoundStopDestroyPacket {
   public ServerBoundStopDestroyPacket() {}

   public ServerBoundStopDestroyPacket(FriendlyByteBuf buffer) {}

   public static void buffer(ServerBoundStopDestroyPacket message, FriendlyByteBuf buffer) {}

   public static void handler(ServerBoundStopDestroyPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player player = context.getSender();
            if (MorphUtils.getEntityLookedAt(player, -1) instanceof PlayerAccessor mob) {
            	mob.removePlayer(player);
            }
		});
		context.setPacketHandled(true);
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ServerBoundStopDestroyPacket.class, 
		ServerBoundStopDestroyPacket::buffer, 
		ServerBoundStopDestroyPacket::new, 
		ServerBoundStopDestroyPacket::handler);
  }
}
