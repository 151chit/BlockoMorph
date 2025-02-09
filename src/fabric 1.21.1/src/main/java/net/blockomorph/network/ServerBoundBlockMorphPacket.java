package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.*;
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
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ServerBoundBlockMorphPacket(CompoundTag morph) implements CustomPacketPayload {
   public static final Type<ServerBoundBlockMorphPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MOD_ID, "server_bound_block_morph_packet"));

   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundBlockMorphPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundBlockMorphPacket message) -> {
		buffer.writeNbt(message.morph);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundBlockMorphPacket(buffer.readNbt()));

   public static void apply(final ServerBoundBlockMorphPacket message, Player player) {
   	      CompoundTag tag = message.morph;
		  try {
            if (player instanceof PlayerAccessor mob) {
            	if (tag == null) throw new IllegalArgumentException("Nbt is null!");
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

   @Override
   public Type<ServerBoundBlockMorphPacket> type() {
		return ID;
   }
   
}
