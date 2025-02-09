package net.blockomorph.network;

import org.apache.logging.log4j.core.appender.rolling.action.Action;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.BlockomorphMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerBoundInteractBlockPacket {
	boolean click;
	int id;
	BlockPos pos;

	public ServerBoundInteractBlockPacket(boolean click, int id, BlockPos pos) {
		this.click = click;
		this.id = id;
		this.pos = pos;
	}

	public ServerBoundInteractBlockPacket(FriendlyByteBuf buffer) {
		this.click = buffer.readBoolean();
		this.id = buffer.readInt();
		this.pos = buffer.readBlockPos();
	}

	public static void buffer(ServerBoundInteractBlockPacket message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.click);
		buffer.writeInt(message.id);
		buffer.writeBlockPos(message.pos);
	}

	public static void handler(ServerBoundInteractBlockPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player player = context.getSender();
			BlockPos pos = message.pos;
			int id = message.id;
			if (pos == null)
				return;
			if (message.click) {
				Entity ent = ((ServerLevel) player.level()).getEntityOrPart(id);
				if (id < 0 || !(ent instanceof Player))
					return;
				MorphUtils.onPlayerAttack(player, ent, pos);
			} else {
				if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor mob) {
					mob.removePlayer(pos, player);
				}
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
			ServerBoundInteractBlockPacket.class, 
			ServerBoundInteractBlockPacket::buffer, 
			ServerBoundInteractBlockPacket::new, 
			ServerBoundInteractBlockPacket::handler
		);
	}
}
