package net.blockomorph.network;

import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.fml.common.Mod;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.*;
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
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

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
		  home:
		  try {
            if (player instanceof PlayerAccessor mob) {
            	CompoundTag tag = message.morph;
            	if (tag == null) throw new IllegalArgumentException("Nbt is null!");
            	if (tag.contains("fuse", 1)) {
            		mob.setTnt();
            		break home;
            	}
            	BlockState blockstate = NbtUtils.readBlockState(player.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            	String reason = MorphUtils.isBannedBlock(blockstate);
            	if (reason.isEmpty()) {
            	    CompoundTag nbt = tag.getCompound("Tags");
            	    if (tag.contains("MultiBlock", 1) && (boolean)Config.getInstance().getValue("advancedMode")) {
            	    	mob.applyBlockMorph(blockstate, nbt, tag.getBoolean("MultiBlock"));
            	    } else {
            	    	mob.applyBlockMorph(blockstate, nbt);
            	    }
            	} else throw new IllegalArgumentException(reason);
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

   public static ServerBoundBlockMorphPacket create(BlockState state, CompoundTag tagMorph, boolean mb) {
   	    CompoundTag tag = new CompoundTag();
		tag.put("BlockState", NbtUtils.writeBlockState(state));
		tag.put("Tags", tagMorph);
		tag.putBoolean("MultiBlock", mb);
		return new ServerBoundBlockMorphPacket(tag);
   }

   public static ServerBoundBlockMorphPacket fuse() {
   	    CompoundTag tag = new CompoundTag();
		tag.putBoolean("fuse", true);
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
