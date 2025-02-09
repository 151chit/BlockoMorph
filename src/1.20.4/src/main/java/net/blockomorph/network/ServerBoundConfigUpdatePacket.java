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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.fml.common.Mod;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record ServerBoundConfigUpdatePacket(String option, String val) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation(BlockomorphMod.MODID, "server_bound_config_update_packet");

   public ServerBoundConfigUpdatePacket(FriendlyByteBuf buffer) {
   	    this(buffer.readUtf(), buffer.readUtf());
   }

   public void write(final FriendlyByteBuf buffer) {
   	    buffer.writeUtf(option);
   	    buffer.writeUtf(val);
   }

   public static void apply(ServerBoundConfigUpdatePacket m, PlayPayloadContext context) {
   	if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.workHandler().submitAsync(() -> {
   	         String op = m.option;
   	         String val = m.val;
   	         Player player = context.player().get();
		     if (player.hasPermissions(2) && (boolean)Config.getInstance().getValue("canOperatorModifyConfig")) {
			     Config.getInstance().parse(op, val, true);
		     }
		  }).exceptionally(e -> {
				context.packetHandler().disconnect(Component.literal(e.getMessage()));
				return null;
		  });
   	}
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
		ID,
		ServerBoundConfigUpdatePacket::new,
		ServerBoundConfigUpdatePacket::apply);
   }

   @Override
   public ResourceLocation id() {
		return ID;
   }
   
}
