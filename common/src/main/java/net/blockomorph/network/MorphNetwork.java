package net.blockomorph.network;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

public class MorphNetwork {
	private static final Object2ObjectOpenHashMap<String, RegisteredPacket> REGISTRY = new Object2ObjectOpenHashMap<>();

	private record RegisteredPacket(Function<FriendlyByteBuf, BlockMorphPacket> ctr, PacketFlow direction) {}

	static void register(String id, Function<FriendlyByteBuf, BlockMorphPacket> ctr, PacketFlow direction) {
		RegisteredPacket pkt = REGISTRY.get(id);
		if (pkt != null) {
			throw new UnsupportedOperationException("Packet with id: " + id + " already registered!");
		}
		REGISTRY.put(id, new RegisteredPacket(ctr, direction));
	}

	public interface MorphPayload {}

	public record BlockomorphCustomPayload(BlockMorphPacket packet) implements CustomPacketPayload, MorphPayload {
		public static final Type<BlockomorphCustomPayload> TYPE = new Type<>(MorphUtils.res("pkt_wrapper"));
		public static final StreamCodec<FriendlyByteBuf, BlockomorphCustomPayload> CODEC = StreamCodec.of((buf, payload) -> {
			buf.writeUtf(payload.packet.getId());
			payload.packet.write(buf);
		}, buf -> {
			var handler = REGISTRY.get(buf.readUtf());
			BlockMorphPacket pkt = null;
			if (handler != null) {
				pkt = handler.ctr.apply(buf);
			}
			return new BlockomorphCustomPayload(pkt);
		});

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ErrorMarkPayload(String error) implements CustomPacketPayload, MorphPayload {
		public static final Type<ErrorMarkPayload> TYPE = new Type<>(MorphUtils.res("pkt_err"));
		public static final StreamCodec<FriendlyByteBuf, ErrorMarkPayload> CODEC = StreamCodec.of((buf, payload) ->
				buf.writeUtf(payload.error()), buf -> new ErrorMarkPayload(buf.readUtf()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static String handleCustomPayload(BlockomorphCustomPayload payload, PacketFlow packetFlow, Player player) {
		BlockMorphPacket packet = payload.packet();
		if (packet == null) {
			return "Empty custom payload container";
		}
		if (player == null) {
			return "Player is null for packet: " + packet.getId();
		}
		var handler = REGISTRY.get(packet.getId());
		if (handler.direction != packetFlow) {
			return "Wrong side for packet: " + packet.getId();
		}
		try {
			packet.handle(player);
		} catch (Throwable e) {
			MorphUtils.LOGGER.error("Error occured while handle blockomoprh packet with id {}", packet.getId(), e);
			return "Packet data error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
		}
		return null;
	}

	public static void sendAll(MinecraftServer server, BlockMorphPacket packet) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.connection.send(packet.toVanillaClientbound());
		}
	}

	static {
		register(ClientBoundConfigUpdatePacket.ID, ClientBoundConfigUpdatePacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundBlockPosBoundPacket.ID, ClientBoundBlockPosBoundPacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundMorphUpdatePacket.ID, ClientBoundMorphUpdatePacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundServerBlockEntityTagPacket.ID, ClientBoundServerBlockEntityTagPacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundApplyBlockMorphPacket.ID, ClientBoundApplyBlockMorphPacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundSyncTntFusePacket.ID, ClientBoundSyncTntFusePacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundSyncTntNbtPacket.ID, ClientBoundSyncTntNbtPacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundChairConnectPacket.ID, ClientBoundChairConnectPacket::new, PacketFlow.CLIENTBOUND);
		register(ClientBoundSerializeInfoPacket.ID, ClientBoundSerializeInfoPacket::new, PacketFlow.CLIENTBOUND);
		register(ServerBoundBlockMorphPacket.ID, ServerBoundBlockMorphPacket::new, PacketFlow.SERVERBOUND);
		register(ServerBoundConfigUpdatePacket.ID, ServerBoundConfigUpdatePacket::new, PacketFlow.SERVERBOUND);
		register(ServerBoundSelfNbtRequestPacket.ID, ignored -> ServerBoundSelfNbtRequestPacket.INSTANCE, PacketFlow.SERVERBOUND);
	}
}
