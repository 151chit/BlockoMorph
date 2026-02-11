package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class MainPacket {
	ResourceLocation id;
	BlockMorphPacket packet;

	public static final RegisterPlatformUtils.PacketCodingHandler<MainPacket> STREAM_CODEC = RegisterPlatformUtils.PacketCodingHandler.of((buffer, message) -> {
		buffer.writeResourceLocation(message.id);
		message.packet.write(buffer);
	}, MainPacket::new);

	public MainPacket(BlockMorphPacket packet) {
		this.id = MorphUtils.res(packet.getId());
		this.packet = packet;
	}

	public MainPacket(FriendlyByteBuf buf) {
		this.id = buf.readResourceLocation();
		MorphUtils.PacketInfo suppl = MorphUtils.getHandler(this.id);
		if (suppl.packet() != null) {
			this.packet = suppl.packet().apply(buf);
		}
	}

	public static void apply(MainPacket message, RegisterPlatformUtils.Context context) {
		MorphUtils.PacketInfo suppl = MorphUtils.getHandler(message.id);
		if (message.packet == null || suppl == null)
			throw new IllegalArgumentException("Unknown packet type received!");

		boolean packetSide = suppl.isClient();
		boolean realSide = context.client();
		if (packetSide != realSide) {
			throw new IllegalArgumentException("Wrong side for packet!");
		}

		message.packet.handle(context.player());
	}

	@Override
	public String toString() {
		return "BlockMorphMainPacket: " + this.id;
	}
}
