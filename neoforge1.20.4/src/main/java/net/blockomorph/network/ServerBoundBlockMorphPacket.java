package net.blockomorph.network;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record ServerBoundBlockMorphPacket(CompoundTag morph) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation(BlockomorphMod.MODID, "server_bound_block_morph_packet");

   public ServerBoundBlockMorphPacket(FriendlyByteBuf buffer) {
   	    this(buffer.readNbt());
   }

   public void write(final FriendlyByteBuf buffer) {
   	    buffer.writeNbt(morph);
   }

   public static void handler(final ServerBoundBlockMorphPacket message, PlayPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.workHandler().submitAsync(() -> {
		   Player player = context.player().get();
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
		  }).exceptionally(e -> {
				context.packetHandler().disconnect(Component.literal(e.getMessage()));
				return null;
		  });
		}
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
		ID,
		ServerBoundBlockMorphPacket::new, 
		ServerBoundBlockMorphPacket::handler);
  }

  @Override
  public ResourceLocation id() {
		return ID;
  }
}
