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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerBoundUseBlockPacket {
    BlockHitResult hit;
    InteractionHand hand;

	public ServerBoundUseBlockPacket(BlockHitResult bl, InteractionHand h) {
		this.hit = bl;
		this.hand = h;
	}

	public ServerBoundUseBlockPacket(FriendlyByteBuf buffer) {
		this.hit = buffer.readBlockHitResult();
		this.hand = buffer.readEnum(InteractionHand.class);
	}

	public static void buffer(ServerBoundUseBlockPacket message, FriendlyByteBuf buffer) {
		buffer.writeBlockHitResult(message.hit);
		buffer.writeEnum(message.hand);
	}

	public static void handler(ServerBoundUseBlockPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player player = context.getSender();
			BlockHitResult hit = message.hit;
			InteractionHand hand = message.hand;
			if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor pl) {
				if (pl.clickPlayer(player, hit, hand).shouldSwing()) player.swing(hand, true);
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		BlockomorphMod.addNetworkMessage(
			ServerBoundUseBlockPacket.class, 
			ServerBoundUseBlockPacket::buffer, 
			ServerBoundUseBlockPacket::new, 
			ServerBoundUseBlockPacket::handler
		);
	}
}
