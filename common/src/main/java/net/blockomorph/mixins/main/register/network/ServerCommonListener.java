package net.blockomorph.mixins.main.register.network;

import io.netty.channel.ChannelFutureListener;
import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonListener {
	@Shadow @Final protected Connection connection;
	@Shadow private volatile boolean suspendFlushingOnServerThread;
	@Shadow @Final protected MinecraftServer server;

	@FastInject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private boolean bypassNeoforge(Packet<?> packet, @Nullable ChannelFutureListener listener) {
		if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload) && payload instanceof MorphNetwork.MorphPayload) {
			try {
				this.connection.send(packet, listener, !this.suspendFlushingOnServerThread || !this.server.isSameThread());
			} catch (Throwable e) {
				CrashReport report = CrashReport.forThrowable(e, "Sending blockomorph packet");
				report.addCategory("Packet being sent").setDetail("Packet class", () -> packet.getClass().getCanonicalName());
				throw new ReportedException(report);
			}
			return false;
		}
		return true;
	}
}
