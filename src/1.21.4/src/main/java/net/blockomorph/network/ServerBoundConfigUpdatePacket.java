package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ServerBoundConfigUpdatePacket(String option, String val) implements CustomPacketPayload {
   public static final Type<ServerBoundConfigUpdatePacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphMod.MODID, "server_bound_config_update_packet"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundConfigUpdatePacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ServerBoundConfigUpdatePacket message) -> {
		buffer.writeUtf(message.option);
   	    buffer.writeUtf(message.val);
	}, (RegistryFriendlyByteBuf buffer) -> new ServerBoundConfigUpdatePacket(buffer.readUtf(), buffer.readUtf()));

   public static void apply(ServerBoundConfigUpdatePacket m, IPayloadContext context) {
   	if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.enqueueWork(() -> {
   	         String op = m.option;
   	         String val = m.val;
		     if (context.player().hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig")) {
			     Config.getInstance().parse(op, val, true);
		     }
		  });
   	}
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID,
		STREAM_CODEC,
		ServerBoundConfigUpdatePacket::apply);
   }

   @Override
   public Type<ServerBoundConfigUpdatePacket> type() {
		return ID;
   }
   
}
