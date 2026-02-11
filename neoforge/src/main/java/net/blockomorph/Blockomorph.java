package net.blockomorph;

import net.blockomorph.core.ClientRegister;
import net.blockomorph.core.CommonRegister;
import net.blockomorph.platformUtilsImpl.NeoRegisterUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.platform.RegisterPlatformUtils;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod(MorphUtils.MODID)
public class Blockomorph {
	private static final String PROTOCOL_VERSION = Util.make(() -> {
		Optional<? extends ModContainer> MOD = ModList.get().getModContainerById(MorphUtils.MODID);
		if (MOD.isPresent()) {
			return MOD.get().getModInfo().getVersion().toString();
		}
		return "unknown";
	});
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MorphUtils.MODID, MorphUtils.MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	public Blockomorph() {
		if (FMLEnvironment.dist == Dist.CLIENT) ClientRegister.register();
		CommonRegister.register();
		int i = 0;
		for (NeoRegisterUtils.PacketForRegister<?> packetForRegister : NeoRegisterUtils.PACKETS) {
			this.registerPacket(packetForRegister, i);
			i++;
		}
	}

	private <T> void registerPacket(NeoRegisterUtils.PacketForRegister<T> packetForRegister, int i) {
		PACKET_HANDLER.registerMessage(i, packetForRegister.type(),
				(msg, buf) -> packetForRegister.codec().encode(buf, msg),
				buf -> packetForRegister.codec().decode(buf),
				(packet, ctx) -> {
			ctx.get().enqueueWork(() -> {
				packetForRegister.handler().accept(packet, new RegisterPlatformUtils.Context(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT, ctx.get().getSender()));
			}).exceptionally(e -> {
				ctx.get().getNetworkManager().disconnect(Component.literal("Broken BlockMorphPacket with ID " + packet + ": " + e.getMessage()));
				return null;
			});
			ctx.get().setPacketHandled(true);
		});
	}

}
