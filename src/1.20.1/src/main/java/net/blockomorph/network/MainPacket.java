package net.blockomorph.network;

import net.blockomorph.Blockomorph;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MainPacket {
    ResourceLocation id;
    BlockMorphPacket packet;

    public MainPacket(BlockMorphPacket packet) {
        this.id = new ResourceLocation(Blockomorph.MODID, packet.getId());
        this.packet = packet;
    }

    public MainPacket(FriendlyByteBuf buf) {
        this.id = buf.readResourceLocation();
        MorphUtils.PacketInfo suppl = MorphUtils.getHandler(this.id);
        if (suppl.packet() != null) {
            this.packet = suppl.packet().apply(buf);
        }
    }

    public static void write(MainPacket message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.id);
        message.packet.write(buffer);
    }

    public static void handler(MainPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            MorphUtils.PacketInfo suppl = MorphUtils.getHandler(message.id);
            if (message.packet == null || suppl == null) throw new IllegalArgumentException("Unknown packet type received!");
            boolean cl = suppl.isClient();
            NetworkDirection dir = context.getDirection();
            if ((dir == NetworkDirection.PLAY_TO_CLIENT && !cl) || (dir == NetworkDirection.PLAY_TO_SERVER && cl)) {
                throw new IllegalArgumentException("Wrong side for packet!");
            }
            message.packet.handle(context.getSender());
        }).exceptionally(e -> {
            context.getNetworkManager().disconnect(Component.literal("Broken BlockMorphPacket with ID " + message.id + ": "+ e.getMessage()));
            e.printStackTrace();
            return null;
        });
        context.setPacketHandled(true);
    }

    @Override
    public String toString() {
        return "BlockMorphMainPacket: " + this.id;
    }

}
