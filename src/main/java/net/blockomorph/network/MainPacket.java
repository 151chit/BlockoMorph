package net.blockomorph.network;

import net.blockomorph.BlockomorphServer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class MainPacket implements CustomPacketPayload {
    public static final Type<MainPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BlockomorphServer.MOD_ID, "main_packet"));
    ResourceLocation id;
    BlockMorphPacket packet;

    public static final StreamCodec<RegistryFriendlyByteBuf, MainPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, MainPacket message) -> {
        buffer.writeResourceLocation(message.id);
        message.packet.write(buffer);
    }, MainPacket::new);

    public MainPacket(BlockMorphPacket packet) {
        this.id = ResourceLocation.fromNamespaceAndPath(BlockomorphServer.MOD_ID, packet.getId());
        this.packet = packet;
    }

    public MainPacket(FriendlyByteBuf buf) {
        this.id = buf.readResourceLocation();
        MorphUtils.PacketInfo suppl = MorphUtils.getHandler(this.id);
        if (suppl.packet() != null) {
            this.packet = suppl.packet().apply(buf);
        }
    }

    public static void apply(MainPacket message, Player player, boolean isClient) {
        MorphUtils.PacketInfo suppl = MorphUtils.getHandler(message.id);
        if (message.packet == null || suppl == null) throw new IllegalArgumentException("Unknown packet type received!");
        boolean cl = suppl.isClient();
        if ((isClient && !cl) || (!isClient && cl)) {
            throw new IllegalArgumentException("Wrong side for packet!");
        }
        message.packet.handle(player);
    }

    @Override
    public String toString() {
        return "BlockMorphMainPacket: " + this.id;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
