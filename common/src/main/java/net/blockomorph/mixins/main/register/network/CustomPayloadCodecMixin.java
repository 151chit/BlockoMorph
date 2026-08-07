package net.blockomorph.mixins.main.register.network;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blockomorph.network.MorphNetwork;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public class CustomPayloadCodecMixin {
	@Unique
	private static final Object2ObjectOpenHashMap<Identifier, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>> MAP = new Object2ObjectOpenHashMap<>(Map.of(
			MorphNetwork.BlockomorphCustomPayload.TYPE.id(), MorphNetwork.BlockomorphCustomPayload.CODEC,
			MorphNetwork.ErrorMarkPayload.TYPE.id(), MorphNetwork.ErrorMarkPayload.CODEC
	));

	@FastInject(method = "findCodec", at = @At("HEAD"))
	private Object testId(Identifier typeId) {
		var codec = MAP.get(typeId);
		if (codec != null) return codec;
		return FastInject.CONTINUE_EXECUTION;
	}
}
