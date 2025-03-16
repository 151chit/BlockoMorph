package net.blockomorph.network;

import net.blockomorph.Blockomorph;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MainPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MainPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Blockomorph.MODID, "main_packet"));
    ResourceLocation id;
    BlockMorphPacket packet;

    public static final StreamCodec<RegistryFriendlyByteBuf, MainPacket> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, MainPacket message) -> {
        buffer.writeResourceLocation(message.id);
        message.packet.write(buffer);
    }, MainPacket::new);

    public MainPacket(BlockMorphPacket packet) {
        this.id = ResourceLocation.fromNamespaceAndPath(Blockomorph.MODID, packet.getId());
        this.packet = packet;
    }

    public MainPacket(FriendlyByteBuf buf) {
        this.id = buf.readResourceLocation();
        MorphUtils.PacketInfo suppl = MorphUtils.getHandler(this.id);
        if (suppl.packet() != null) {
            this.packet = suppl.packet().apply(buf);
        }
    }

    public static void apply(MainPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            MorphUtils.PacketInfo suppl = MorphUtils.getHandler(message.id);
            if (message.packet == null || suppl == null) throw new IllegalArgumentException("Unknown packet type received!");
            boolean cl = suppl.isClient();
            PacketFlow dir = context.flow();
            if ((dir == PacketFlow.CLIENTBOUND && !cl) || (dir == PacketFlow.SERVERBOUND && cl)) {
                throw new IllegalArgumentException("Wrong side for packet!");
            }
            message.packet.handle(context.player());
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal("Broken BlockMorphPacket with ID " + message.id + ": "+ e.getMessage()));
            return null;
        });
    }

    @Override
    public String toString() {
        return "BlockMorphMainPacket: " + this.id;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    //res instanceof InteractionResult.Success s && s.swingSource() == InteractionResult.SwingSource.SERVER
}
