package net.blockomorph.network;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.fml.common.Mod;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.config.*;

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
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerBoundConfigUpdatePacket {
   String option;
   String value;
   
   public ServerBoundConfigUpdatePacket(String option, String val) {
   	    this.option = option;
   	    this.value = val;
   }

   public ServerBoundConfigUpdatePacket(FriendlyByteBuf buffer) {
   	    this.option = buffer.readUtf();
   	    this.value = buffer.readUtf();
   }

   public static void buffer(ServerBoundConfigUpdatePacket message, FriendlyByteBuf buffer) {
   	    buffer.writeUtf(message.option);
   	    buffer.writeUtf(message.value);
   }

   public static void handler(ServerBoundConfigUpdatePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig")) {
				Config.getInstance().parse(message.option, message.value, true);
			}
		});
		context.setPacketHandled(true);
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ServerBoundConfigUpdatePacket.class, 
		ServerBoundConfigUpdatePacket::buffer, 
		ServerBoundConfigUpdatePacket::new, 
		ServerBoundConfigUpdatePacket::handler);
  }
}
