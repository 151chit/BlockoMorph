package net.blockomorph.network;

import net.blockomorph.BlockomorphMod;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record ServerBoundInteractBlockPacket(boolean click, int id, BlockPos pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation(BlockomorphMod.MODID, "server_bound_interact_block_packet");

   public ServerBoundInteractBlockPacket(FriendlyByteBuf buffer) {
            this(buffer.readBoolean(), buffer.readInt(), buffer.readBlockPos());
   }

   public void write(final FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.click);
   	    buffer.writeInt(message.id);
   	    buffer.writeBlockPos(message.pos);

   public static void handler(final ServerBoundInteractBlockPacket message, final PlayPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
		  context.workHandler().submitAsync(() -> {
			Player player = context.player().get();
            BlockPos pos = message.pos;
			int id = message.id;
            if (pos == null) return;
            if (message.click) {
            	Entity ent = ((ServerLevel)player.level()).getEntityOrPart(id);
            	if (id < 0 || !(ent instanceof Player)) return;
            	MorphUtils.onPlayerAttack(player, ent, pos);
            } else {
            	if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor mob) {
            		mob.removePlayer(pos, player);
            	}
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
		ServerBoundInteractBlockPacket::new, 
		ServerBoundInteractBlockPacket::handler);
  }

  @Override
  public ResourceLocation id() {
		return ID;
  }
}
