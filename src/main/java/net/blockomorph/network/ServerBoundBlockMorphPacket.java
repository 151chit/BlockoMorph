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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerBoundBlockMorphPacket {
   CompoundTag morph;
   public ServerBoundBlockMorphPacket(CompoundTag tag) {
   	    this.morph = tag;
   }

   public ServerBoundBlockMorphPacket(FriendlyByteBuf buffer) {
   	    this.morph = buffer.readAnySizeNbt();
   }

   public static void buffer(ServerBoundBlockMorphPacket message, FriendlyByteBuf buffer) {
   	    buffer.writeNbt(message.morph);
   }

   public static void handler(ServerBoundBlockMorphPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
		  Player player = context.getSender();
		  try {
            if (player instanceof PlayerAccessor mob) {
            	CompoundTag tag = message.morph;
            	if (tag == null) throw new IllegalArgumentException("Nbt is null!");
            	BlockState blockstate = NbtUtils.readBlockState(player.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            	CompoundTag nbt = tag.getCompound("Tags");
            	mob.applyBlockMorph(blockstate, nbt);
            }
		  } catch (Exception e) {
		  	BlockomorphMod.LOGGER.warn("Invalid block morph nbt from player " + player + ": " + e.getMessage());
		  }
		});
		context.setPacketHandled(true);
   }

   public static ServerBoundBlockMorphPacket create(BlockState state, CompoundTag tagMorph) {
   	    CompoundTag tag = new CompoundTag();
		tag.put("BlockState", NbtUtils.writeBlockState(state));
		tag.put("Tags", tagMorph);
		return new ServerBoundBlockMorphPacket(tag);
   }
   
   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ServerBoundBlockMorphPacket.class, 
		ServerBoundBlockMorphPacket::buffer, 
		ServerBoundBlockMorphPacket::new, 
		ServerBoundBlockMorphPacket::handler);
  }
}
