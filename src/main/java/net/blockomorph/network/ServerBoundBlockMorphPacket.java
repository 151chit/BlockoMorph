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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Supplier;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ServerBoundBlockMorphPacket(CompoundTag morph) implements CustomPacketPayload {
   public static final Type<ServerBoundBlockMorphPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MODID, "server_bound_block_morph_packet"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundBlockMorphPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundBlockMorphPacket message) -> {
		buffer.writeNbt(message.morph);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundBlockMorphPacket(buffer.readNbt()));

   public static void handler(final ServerBoundBlockMorphPacket message, IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.enqueueWork(() -> {
		   Player player = context.player();
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
				context.connection().disconnect(Component.literal(e.getMessage()));
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
		STREAM_CODEC,
		ServerBoundBlockMorphPacket::handler);
  }

  @Override
  public Type<ServerBoundBlockMorphPacket> type() {
		return ID;
  }
}
