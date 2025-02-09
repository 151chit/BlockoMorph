package net.blockomorph.network;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.fml.common.Mod;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.config.*;
import net.blockomorph.screens.MorphScreen;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientBoundConfigUpdatePacket {
   Config conf;
   public ClientBoundConfigUpdatePacket(Config c) {
   	    this.conf = c;
   }

   public ClientBoundConfigUpdatePacket(FriendlyByteBuf buffer) {
   	    Config.readFromBufer(buffer);
   }

   public static void buffer(ClientBoundConfigUpdatePacket message, FriendlyByteBuf buffer) {
   	    message.conf.writeInBufer(buffer);
   }

   public static void handler(ClientBoundConfigUpdatePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			if (Minecraft.getInstance().screen instanceof MorphScreen s) {
				s.updateAllowed();
			}
		});
		context.setPacketHandled(true);
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ClientBoundConfigUpdatePacket.class, 
		ClientBoundConfigUpdatePacket::buffer, 
		ClientBoundConfigUpdatePacket::new, 
		ClientBoundConfigUpdatePacket::handler);
  }
}
