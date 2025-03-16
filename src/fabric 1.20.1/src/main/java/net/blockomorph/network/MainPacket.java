package net.blockomorph.network;

import io.netty.buffer.Unpooled;
import net.blockomorph.BlockomorphServer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class MainPacket extends FriendlyByteBuf {
    public static final ResourceLocation ID = new ResourceLocation(BlockomorphServer.MOD_ID, "main_packet");
    public MainPacket(BlockMorphPacket packet) {
        super(Unpooled.buffer());
        writeResourceLocation(new ResourceLocation(BlockomorphServer.MOD_ID, packet.getId()));
        packet.write(this);
    }

    public static Runnable preApply(FriendlyByteBuf buf, Player player, boolean isClient) {
        ResourceLocation res = buf.readResourceLocation();
        MorphUtils.PacketInfo suppl = MorphUtils.getHandler(res);
        if (suppl == null || suppl.packet() == null) throw new IllegalArgumentException("Unknown packet type received!");
        BlockMorphPacket packet = suppl.packet().apply(buf);
        return () -> {
            postApply(packet, player, isClient, suppl.isClient());
        };
    }

    private static void postApply(BlockMorphPacket packet, Player player, boolean isClient, boolean client) {
        if (packet == null) throw new IllegalArgumentException("Unknown packet type received!");
        if ((isClient && !client) || (!isClient && client)) {
            throw new IllegalArgumentException("Wrong side for packet!");
        }
        packet.handle(player);
    }
}
