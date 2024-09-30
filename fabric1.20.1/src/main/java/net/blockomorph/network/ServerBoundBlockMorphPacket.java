package net.blockomorph.network;

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
import io.netty.buffer.Unpooled;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

public class ServerBoundBlockMorphPacket extends FriendlyByteBuf {
   public ServerBoundBlockMorphPacket(CompoundTag tag) {
   	    super(Unpooled.buffer());
   	    writeNbt(tag);
   }

   public static void apply(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
   	    CompoundTag tag = buf.readAnySizeNbt();
		server.execute(() -> {
		  try {
            if (player instanceof PlayerAccessor mob) {
            	if (tag == null) throw new IllegalArgumentException("Nbt is null!");
            	BlockState blockstate = NbtUtils.readBlockState(player.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            	CompoundTag nbt = tag.getCompound("Tags");
            	mob.applyBlockMorph(blockstate, nbt);
            }
		  } catch (Exception e) {
		  	BlockomorphMod.LOGGER.warn("Invalid block morph nbt from player " + player + ": " + e.getMessage());
		  }
		});
   }

   public static ServerBoundBlockMorphPacket create(BlockState state, CompoundTag tagMorph) {
   	    CompoundTag tag = new CompoundTag();
		tag.put("BlockState", NbtUtils.writeBlockState(state));
		tag.put("Tags", tagMorph);
		return new ServerBoundBlockMorphPacket(tag);
   }
   
}
